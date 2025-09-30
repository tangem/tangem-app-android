package com.tangem.data.pay.repository

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class TangemPayRequestPerformer @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayStorage: TangemPayStorage,
    private val networksRepository: NetworksRepository,
    private val authDataSource: TangemPayAuthDataSource,
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) {

    private var customerWalletAddress: String? = null

    private val refreshTokensMutex = Mutex()
    private var refreshTokensJob: Deferred<VisaAuthTokens>? = null

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    suspend fun <T : Any> runWithErrorLogs(tag: String, requestBlock: suspend () -> T): Either<UniversalError, T> {
        return try {
            val result = requestBlock()
            Either.Right(result)
        } catch (exception: Exception) {
            Timber.e("$tag: $exception")
            Either.Left(mapError(exception))
        }
    }

    suspend fun <T : Any> request(requestBlock: suspend (header: String) -> ApiResponse<T>): T =
        withContext(dispatchers.io) {
            performRequest(
                requestBlock = requestBlock,
                getTokens = ::getAccessTokens,
                refreshTokens = ::refreshAuthTokens,
            )
        }

    suspend fun <T : Any> requestWithPersistedToken(requestBlock: suspend (header: String) -> ApiResponse<T>): T =
        withContext(dispatchers.io) {
            performRequest(
                requestBlock = requestBlock,
                getTokens = { getAccessTokensIfSaved() ?: error("Cannot get saved access tokens") },
                refreshTokens = ::refreshAuthTokens,
            )
        }

    private fun getWallets(): Flow<List<UserWallet>> = if (hotWalletFeatureToggles.isHotWalletEnabled) {
        userWalletsListRepository.userWallets.map { requireNotNull(it) }
    } else {
        userWalletsListManager.userWallets
    }

    private suspend fun <T : Any> performRequest(
        requestBlock: suspend (header: String) -> ApiResponse<T>,
        getTokens: (suspend () -> VisaAuthTokens),
        refreshTokens: (suspend () -> VisaAuthTokens)? = null,
    ): T = runCatching {
        requestBlock("Bearer ${getTokens().accessToken}").getOrThrow()
    }.getOrElse { error ->
        val unauthorizedCode = ApiResponseError.HttpException.Code.UNAUTHORIZED
        if (error is ApiResponseError.HttpException && refreshTokens != null && error.code == unauthorizedCode) {
            refreshOrJoin(refreshTokens)
            performRequest(requestBlock, refreshTokens = null, getTokens = getTokens)
        } else {
            throw error
        }
    }

    private suspend fun refreshOrJoin(refreshTokens: suspend () -> VisaAuthTokens): VisaAuthTokens {
        val jobToAwait: Deferred<VisaAuthTokens> =
            refreshTokensMutex.withLock {
                val current = refreshTokensJob
                if (current == null || current.isCompleted) {
                    coroutineScope {
                        async { refreshTokens() }.also { refreshTokensJob = it }
                    }
                } else {
                    current
                }
            }
        val result = try {
            jobToAwait.await()
        } finally {
            refreshTokensMutex.withLock {
                if (refreshTokensJob === jobToAwait && jobToAwait.isCompleted) {
                    refreshTokensJob = null
                }
            }
        }
        return result
    }

    suspend fun getCustomerWalletAddress(): String = customerWalletAddress ?: fetchAuthInputData().address

    private suspend fun getAccessTokens(): VisaAuthTokens {
        return getAccessTokensIfSaved() ?: fetchTokens()
    }

    private suspend fun getAccessTokensIfSaved(): VisaAuthTokens? {
        return tangemPayStorage.getAuthTokens(getCustomerWalletAddress())
    }

    private suspend fun fetchAuthInputData(): AuthInputData {
        val userWallets = getWallets()
            .filter { it.isNotEmpty() }
            .first()
        val wallet = userWallets.find { it is UserWallet.Cold } as? UserWallet.Cold
            ?: error("Cannot find cold user wallet")

        val blockchain = Blockchain.Polygon
        val derivationPath = getDerivationPath(blockchain, wallet)
        val address = networksRepository.getNetworkAddresses(wallet.walletId, Network.RawID(blockchain.id))
            .find { it.cryptoCurrency.network.derivationPath.value == derivationPath }?.address
            ?: error("Cannot get polygon address")

        customerWalletAddress = address

        return AuthInputData(address, wallet.cardId)
    }

    private fun getDerivationPath(blockchain: Blockchain, wallet: UserWallet) =
        blockchain.derivationPath(wallet.derivationStyleProvider.getDerivationStyle())?.rawPath
            ?: error("Cannot get derivation path")

    private suspend fun fetchTokens(): VisaAuthTokens {
        val inputData = fetchAuthInputData()
        val tokens = authDataSource.generateNewAuthTokens(inputData.address, inputData.cardId)
            .getOrNull() ?: error("Cannot fetch tokens")
        tangemPayStorage.storeAuthTokens(inputData.address, tokens)
        return tokens
    }

    private suspend fun refreshAuthTokens(): VisaAuthTokens {
        val customerWalletAddress = getCustomerWalletAddress()
        val refreshToken = getAccessTokens().refreshToken.value
        val tokens = authDataSource.refreshAuthTokens(refreshToken).getOrNull() ?: error("Cannot refresh tokens")
        tangemPayStorage.storeAuthTokens(customerWalletAddress, tokens)
        return tokens
    }

    private fun mapError(throwable: Throwable): UniversalError {
        return if (throwable is ApiResponseError.HttpException) {
            val errorBody = throwable.errorBody ?: return VisaApiError.UnknownWithoutCode
            return runCatching {
                visaErrorAdapter.fromJson(errorBody)?.error?.code ?: throwable.code.numericCode
            }.map {
                VisaApiError.fromBackendError(it)
            }.getOrElse {
                VisaApiError.UnknownWithoutCode
            }
        } else {
            VisaApiError.UnknownWithoutCode
        }
    }
}

internal data class AuthInputData(
    val address: String,
    val cardId: String,
)