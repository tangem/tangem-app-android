package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import com.squareup.moshi.Moshi
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * For TangemPay Customer Wallet auth we are using polygon address
 */
private const val POL_VALUE = "coin⟨POLYGON⟩polygon-ecosystem-token"

internal class TangemPayRequestPerformer @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayStorage: TangemPayStorage,
    private val userWalletsRepository: UserWalletsListRepository,
    private val getCurrencyUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val authDataSource: TangemPayAuthDataSource,
) {
    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    private var customerWalletAddress: String? = null

    private val refreshTokensMutex = Mutex()
    private var refreshTokensJob: Deferred<Either<UniversalError, VisaAuthTokens>>? = null

    suspend fun <T : Any> request(requestBlock: suspend (header: String) -> T): Either<UniversalError, T> = either {
        withContext(dispatchers.io) {
            performRequest(requestBlock = requestBlock, refreshTokens = ::refreshAuthTokens).bind()
        }
    }

    private suspend fun <T : Any> performRequest(
        requestBlock: suspend (header: String) -> T,
        refreshTokens: (suspend () -> Either<UniversalError, VisaAuthTokens>)? = null,
    ): Either<UniversalError, T> = either {
        runCatching {
            requestBlock("Bearer ${getAccessTokens().bind().accessToken}")
        }.getOrElse { error ->
            when (error) {
                is ApiResponseError.HttpException -> {
                    if (refreshTokens != null && error.code == ApiResponseError.HttpException.Code.UNAUTHORIZED) {
                        refreshOrJoin(refreshTokens).bind()
                        performRequest(requestBlock, refreshTokens = null).bind()
                    } else {
                        raise(mapHttpError(error))
                    }
                }
                else -> raise(VisaApiError.UnknownWithoutCode)
            }
        }
    }

    private suspend fun refreshOrJoin(
        refreshTokens: suspend () -> Either<UniversalError, VisaAuthTokens>,
    ): Either<UniversalError, VisaAuthTokens> {
        val jobToAwait: Deferred<Either<UniversalError, VisaAuthTokens>> =
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
        } catch (ignore: Throwable) {
            Either.Left(VisaApiError.UnknownWithoutCode)
        } finally {
            refreshTokensMutex.withLock {
                if (refreshTokensJob === jobToAwait && jobToAwait.isCompleted) {
                    refreshTokensJob = null
                }
            }
        }
        return result
    }

    private suspend fun getCustomerWalletAddress(): Either<UniversalError, String> = either {
        customerWalletAddress
            ?: tangemPayStorage.getCustomerWalletAddress()
            ?: fetchAuthInputData().bind().address
    }

    private suspend fun getAccessTokens(): Either<UniversalError, VisaAuthTokens> = either {
        val address = getCustomerWalletAddress().bind()
        tangemPayStorage.getAuthTokens(address) ?: fetchTokens().bind()
    }

    private fun mapHttpError(throwable: ApiResponseError.HttpException): UniversalError {
        val errorBody = throwable.errorBody ?: return VisaApiError.UnknownWithoutCode
        return runCatching {
            visaErrorAdapter.fromJson(errorBody)?.error?.code ?: throwable.code.numericCode
        }.map {
            VisaApiError.fromBackendError(it)
        }.getOrElse {
            VisaApiError.UnknownWithoutCode
        }
    }

    private suspend fun fetchAuthInputData(): Either<UniversalError, AuthInputData> = either {
        val wallet = userWalletsRepository.userWalletsSync().find { it is UserWallet.Cold } as? UserWallet.Cold
            ?: raise(VisaApiError.UnknownWithoutCode)

        val address = getCurrencyUseCase.invokeMultiWalletSync(wallet.walletId, CryptoCurrency.ID.fromValue(POL_VALUE))
            .getOrNull()?.value?.networkAddress?.defaultAddress?.value ?: raise(VisaApiError.UnknownWithoutCode)

        customerWalletAddress = address
        tangemPayStorage.storeCustomerWalletAddress(address)

        AuthInputData(address, wallet.cardId)
    }

    private suspend fun fetchTokens(): Either<UniversalError, VisaAuthTokens> = either {
        val inputData = fetchAuthInputData().bind()
        val tokens = authDataSource.generateNewAuthTokens(inputData.address, inputData.cardId)
            .getOrNull()
            ?: return Either.Left(VisaApiError.UnknownWithoutCode)
        tangemPayStorage.storeAuthTokens(inputData.address, tokens)
        tokens
    }

    private suspend fun refreshAuthTokens(): Either<UniversalError, VisaAuthTokens> = either {
        val customerWalletAddress = getCustomerWalletAddress().bind()
        val refreshToken = getAccessTokens().bind().refreshToken.value
        val tokens = authDataSource.refreshAuthTokens(refreshToken).getOrNull()
            ?: raise(VisaApiError.UnknownWithoutCode)
        tangemPayStorage.storeAuthTokens(customerWalletAddress, tokens)
        tokens
    }
}

internal data class AuthInputData(
    val address: String,
    val cardId: String,
)