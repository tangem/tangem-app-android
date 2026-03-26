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
import com.tangem.domain.payment.models.auth.PaymentAuthApiError
import com.tangem.domain.payment.auth.PaymentRemoteDataSource
import com.tangem.domain.payment.models.auth.PaymentAuthChallenge
import com.tangem.domain.payment.models.auth.PaymentAuthSession
import com.tangem.domain.payment.models.auth.PaymentAuthTokens
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

internal class TangemPayRemoteDataSource @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    private val tangemPayAuthApi: TangemPayAuthApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PaymentRemoteDataSource {

    private val errorAdapter by lazy { moshi.adapter(TangemPayErrorResponse::class.java) }

    override suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
        customerWalletId: String,
    ): Either<PaymentAuthApiError, PaymentAuthChallenge> = withContext(dispatchers.io) {
        request {
            tangemPayAuthApi.generateNonceByCustomerWallet(
                request = GenerateNonceByCustomerWalletRequest(
                    customerWalletAddress = customerWalletAddress,
                    customerWalletId = customerWalletId,
                ),
            ).getOrThrow()
        }.map { response ->
            PaymentAuthChallenge(
                challenge = response.nonce,
                session = PaymentAuthSession(response.sessionId),
            )
        }
    }

    override suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<PaymentAuthApiError, PaymentAuthTokens> = withContext(dispatchers.io) {
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
            PaymentAuthTokens(
                accessToken = response.accessToken,
                expiresAt = response.expiresAt,
                refreshToken = response.refreshToken,
                refreshExpiresAt = response.refreshExpiresAt,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        }
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): Either<PaymentAuthApiError, T> {
        return runCatching {
            Either.Right(requestBlock())
        }.getOrElse { responseError ->
            if (responseError is ApiResponseError.HttpException &&
                responseError.errorBody != null
            ) {
                val errorCode =
                    errorAdapter.fromJson(responseError.errorBody)?.error?.code ?: responseError.code.numericCode
                return Either.Left(PaymentAuthApiError.fromBackendError(errorCode))
            }

            return Either.Left(PaymentAuthApiError.UnknownWithoutCode)
        }
    }
}