package com.tangem.data.tangempay

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.spend.datasource.tangempay.TangemPayAuthApi
import com.tangem.spend.datasource.tangempay.models.request.GenerateNonceByCustomerWalletRequest
import com.tangem.spend.datasource.tangempay.models.request.GetTokenByCustomerWalletRequest
import com.tangem.spend.datasource.tangempay.models.response.TangemPayErrorResponse
import com.tangem.core.remote.json.NetworkJson
import com.tangem.domain.card.common.tangempay.TangemPayUtilities
import com.tangem.domain.tangempay.datasource.TangemPayRemoteDataSource
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayAuthTokens
import com.tangem.domain.tangempay.model.TangemPayAuthChallenge
import com.tangem.domain.tangempay.model.TangemPayAuthSession
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

internal class DefaultTangemPayRemoteDataSource @Inject constructor(
    @NetworkJson private val json: Json,
    private val tangemPayAuthApi: TangemPayAuthApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : TangemPayRemoteDataSource {

    override suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
        customerWalletId: String,
    ): Either<TangemPayApiError, TangemPayAuthChallenge> = withContext(dispatchers.io) {
        request {
            tangemPayAuthApi.generateNonceByCustomerWallet(
                request = GenerateNonceByCustomerWalletRequest(
                    customerWalletAddress = customerWalletAddress,
                    customerWalletId = customerWalletId,
                ),
            ).getOrThrow()
        }.map { response ->
            TangemPayAuthChallenge(
                challenge = response.nonce,
                session = TangemPayAuthSession(response.sessionId),
            )
        }
    }

    override suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<TangemPayApiError, TangemPayAuthTokens> = withContext(dispatchers.io) {
        request {
            tangemPayAuthApi.getTokenByCustomerWallet(
                request = GetTokenByCustomerWalletRequest(
                    authType = "customer_wallet",
                    sessionId = sessionId,
                    signature = signature,
                    messageFormat = TangemPayUtilities.signWithNonceMessage(nonce),
                ),
            ).getOrThrow()
        }.map { response ->
            TangemPayAuthTokens(
                accessToken = response.accessToken,
                expiresAt = response.expiresAt,
                refreshToken = response.refreshToken,
                refreshExpiresAt = response.refreshExpiresAt,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        }
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): Either<TangemPayApiError, T> {
        return runCatching {
            Either.Right(requestBlock())
        }.getOrElse { responseError ->
            if (responseError is ApiResponseError.HttpException &&
                responseError.errorBody != null
            ) {
                val errorCode = TangemPayErrorResponse.decodeOrNull(json, responseError.errorBody)?.error?.code
                    ?: responseError.code.numericCode
                return Either.Left(TangemPayApiError.fromBackendError(errorCode))
            }

            return Either.Left(TangemPayApiError.UnknownWithoutCode)
        }
    }
}