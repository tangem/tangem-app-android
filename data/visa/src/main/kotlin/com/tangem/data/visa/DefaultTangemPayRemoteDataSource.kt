package com.tangem.data.visa

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayAuthApi
import com.tangem.datasource.api.pay.models.request.GenerateNonceByCustomerWalletRequest
import com.tangem.datasource.api.pay.models.request.GetTokenByCustomerWalletRequest
import com.tangem.datasource.api.pay.models.response.TangemPayErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.visa.datasource.TangemPayRemoteDataSource
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaAuthSession
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultTangemPayRemoteDataSource @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    private val tangemPayAuthApi: TangemPayAuthApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : TangemPayRemoteDataSource {

    private val errorAdapter by lazy { moshi.adapter(TangemPayErrorResponse::class.java) }

    override suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
        customerWalletId: String,
    ): Either<VisaApiError, VisaAuthChallenge.Wallet> = withContext(dispatchers.io) {
        request {
            tangemPayAuthApi.generateNonceByCustomerWallet(
                request = GenerateNonceByCustomerWalletRequest(
                    customerWalletAddress = customerWalletAddress,
                    customerWalletId = customerWalletId,
                ),
            ).getOrThrow()
        }.map { response ->
            VisaAuthChallenge.Wallet(
                challenge = response.nonce,
                session = VisaAuthSession(response.sessionId),
            )
        }
    }

    override suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<VisaApiError, TangemPayAuthTokens> = withContext(dispatchers.io) {
        request {
            tangemPayAuthApi.getTokenByCustomerWallet(
                request = GetTokenByCustomerWalletRequest(
                    authType = "customer_wallet",
                    sessionId = sessionId,
                    signature = signature,
                    messageFormat = VisaUtilities.signWithNonceMessage(nonce),
                ),
            ).getOrThrow()
        }.map { response ->
            TangemPayAuthTokens(
                accessToken = response.accessToken,
                expiresAt = response.expiresAt,
                refreshToken = response.refreshToken,
                refreshExpiresAt = response.refreshExpiresAt,
            )
        }
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): Either<VisaApiError, T> {
        return runCatching {
            Either.Right(requestBlock())
        }.getOrElse { responseError ->
            if (responseError is ApiResponseError.HttpException &&
                responseError.errorBody != null
            ) {
                val errorCode =
                    errorAdapter.fromJson(responseError.errorBody)?.error?.code ?: responseError.code.numericCode
                return Either.Left(VisaApiError.fromBackendError(errorCode))
            }

            return Either.Left(VisaApiError.UnknownWithoutCode)
        }
    }
}