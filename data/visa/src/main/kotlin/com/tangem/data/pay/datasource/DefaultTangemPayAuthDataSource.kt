package com.tangem.data.pay.datasource

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.WithdrawalSignatureResult
import com.tangem.domain.visa.datasource.VisaAuthRemoteDataSource
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

internal class DefaultTangemPayAuthDataSource @Inject constructor(
    private val visaAuthRemoteDataSource: VisaAuthRemoteDataSource,
    private val tangemSdkManager: TangemSdkManager,
) : TangemPayAuthDataSource {

    override suspend fun produceInitialCredentials(cardId: String): Either<Throwable, TangemPayInitialCredentials> {
        return when (val initialCredentials = tangemSdkManager.tangemPayProduceInitialCredentials(cardId = cardId)) {
            is CompletionResult.Failure<*> -> Either.Left(initialCredentials.error)
            is CompletionResult.Success<TangemPayInitialCredentials> -> Either.Right(initialCredentials.data)
        }
    }

    override suspend fun refreshAuthTokens(refreshToken: String): Either<Throwable, TangemPayAuthTokens> = either {
        visaAuthRemoteDataSource.refreshCustomerWalletAuthTokens(refreshToken = refreshToken)
            .mapLeft { IllegalStateException("TangemPay token refresh failed. Error code: ${it.errorCode}") }
            .bind()
    }

    override suspend fun getWithdrawalSignature(
        cardId: String,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult> {
        return when (val signResult = tangemSdkManager.getWithdrawalSignature(cardId, hash)) {
            is CompletionResult.Failure<*> -> {
                if (signResult.error is TangemSdkError.UserCancelled) {
                    Either.Right(WithdrawalSignatureResult.Cancelled)
                } else {
                    signResult.error.left()
                }
            }
            is CompletionResult.Success<String> -> {
                Either.Right(WithdrawalSignatureResult.Success(signResult.data))
            }
        }
    }
}