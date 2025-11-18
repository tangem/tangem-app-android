package com.tangem.data.pay.repository

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.error.UniversalError
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.data.pay.util.TangemPayWalletsManager
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class TangemPayRequestPerformer @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayStorage: TangemPayStorage,
    private val authDataSource: TangemPayAuthDataSource,
    private val tangemPayWalletsManager: TangemPayWalletsManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val networkFactory: NetworkFactory,
) {

    private var customerWalletAddress: String? = null

    private val refreshTokensMutex = Mutex()
    private var refreshTokensJob: Deferred<VisaAuthTokens>? = null

    private val errorConverter = TangemPayErrorConverter(moshi)

    suspend fun <T : Any> runWithErrorLogs(tag: String, requestBlock: suspend () -> T): Either<UniversalError, T> {
        return try {
            val result = requestBlock()
            Either.Right(result)
        } catch (exception: Exception) {
            when (exception) {
                is CancellationException -> {
                    throw exception
                }
                else -> {
                    Timber.tag(tag).e(exception)
                    Either.Left(errorConverter.convert(exception))
                }
            }
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
        val wallet = tangemPayWalletsManager.getDefaultWalletForTangemPay()

        val network = networkFactory.create(
            blockchain = Blockchain.Polygon,
            extraDerivationPath = null,
            derivationStyleProvider = wallet.derivationStyleProvider,
            canHandleTokens = true,
        ) ?: error("Cannot create network")

        val address = walletManagersFacade.getDefaultAddress(wallet.walletId, network)
            ?: error("Cannot get polygon address")

        customerWalletAddress = address

        return AuthInputData(address, wallet.cardId)
    }

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
}

internal data class AuthInputData(
    val address: String,
    val cardId: String,
)