package com.tangem.data.pay.datasource

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.WithdrawalSignatureResult
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

internal class DefaultTangemPayAuthDataSource @Inject constructor(
    private val tangemSdkManager: TangemSdkManager,
) : TangemPayAuthDataSource {

    override suspend fun produceInitialCredentials(cardId: String): Either<Throwable, TangemPayInitialCredentials> {
        return when (val initialCredentials = tangemSdkManager.tangemPayProduceInitialCredentials(cardId = cardId)) {
            is CompletionResult.Failure<*> -> initialCredentials.error.left()
            is CompletionResult.Success<TangemPayInitialCredentials> -> initialCredentials.data.right()
        }
    }

    override suspend fun getWithdrawalSignature(
        cardId: String,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult> {
        return when (val signResult = tangemSdkManager.getWithdrawalSignature(cardId, hash)) {
            is CompletionResult.Failure<*> -> {
                if (signResult.error is TangemSdkError.UserCancelled) {
                    WithdrawalSignatureResult.Cancelled.right()
                } else {
                    signResult.error.left()
                }
            }
            is CompletionResult.Success<String> -> {
                WithdrawalSignatureResult.Success(signResult.data).right()
            }
        }
    }
}