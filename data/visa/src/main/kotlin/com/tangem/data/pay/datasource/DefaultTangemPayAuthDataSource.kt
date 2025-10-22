package com.tangem.data.pay.datasource

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.CompletionResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.datasource.VisaAuthRemoteDataSource
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

internal class DefaultTangemPayAuthDataSource @Inject constructor(
    private val visaAuthRemoteDataSource: VisaAuthRemoteDataSource,
    private val tangemSdkManager: TangemSdkManager,
) : TangemPayAuthDataSource {

    override suspend fun produceInitialCredentials(cardId: String): Either<Throwable, TangemPayInitialCredentials> {
        val initialCredentials = tangemSdkManager.tangemPayProduceInitialCredentials(cardId = cardId)

        return when (initialCredentials) {
            is CompletionResult.Failure<*> -> Either.Left(initialCredentials.error)
            is CompletionResult.Success<TangemPayInitialCredentials> -> Either.Right(initialCredentials.data)
        }
    }

    override suspend fun refreshAuthTokens(refreshToken: String): Either<Throwable, VisaAuthTokens> = either {
        visaAuthRemoteDataSource.refreshCustomerWalletAuthTokens(
            VisaAuthTokens.RefreshToken(refreshToken, authType = VisaAuthTokens.RefreshToken.Type.CardWallet),
        )
            .mapLeft { IllegalStateException("TangemPay token refresh failed. Error code: ${it.errorCode}") }
            .bind()
    }
}