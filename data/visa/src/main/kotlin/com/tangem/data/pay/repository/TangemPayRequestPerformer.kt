package com.tangem.data.pay.repository

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * For TangemPay Customer Wallet auth we are using polygon address
 */
private const val POL_VALUE = "coin⟨POLYGON⟩polygon-ecosystem-token"

internal class TangemPayRequestPerformer @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayStorage: TangemPayStorage,
    private val getCurrencyUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val authDataSource: TangemPayAuthDataSource,
    private val getWalletsUseCase: GetWalletsUseCase,
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
        val userWallets = getWalletsUseCase()
            .filter { it.isNotEmpty() }
            .first()
        val wallet = userWallets.find { it is UserWallet.Cold } as? UserWallet.Cold
            ?: error("Cannot find cold user wallet")

        val address = getCurrencyUseCase.invokeMultiWallet(
            userWalletId = wallet.walletId,
            currencyId = CryptoCurrency.ID.fromValue(POL_VALUE),
            isSingleWalletWithTokens = false,
        )
            .filter { it.getAddress() != null }.first().getAddress() ?: error("Cannot find polygon network address")

        customerWalletAddress = address

        return AuthInputData(address, wallet.cardId)
    }

    private fun Either<CurrencyStatusError, CryptoCurrencyStatus>.getAddress() =
        getOrNull()?.value?.networkAddress?.defaultAddress?.value

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