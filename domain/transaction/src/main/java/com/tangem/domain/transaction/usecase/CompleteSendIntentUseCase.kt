package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.transaction.SendIntentRepository

class CompleteSendIntentUseCase(
    private val sendIntentRepository: SendIntentRepository,
) {

    suspend operator fun invoke(intentId: String, txHash: String): Either<Throwable, Unit> {
        return sendIntentRepository.completeSendIntent(intentId = intentId, txHash = txHash)
    }
}