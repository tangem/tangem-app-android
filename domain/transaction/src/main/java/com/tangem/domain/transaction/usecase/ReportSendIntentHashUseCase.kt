package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.transaction.SendIntentRepository

/** Tells the service the hash of the transaction a send intent ended in, trying again when the service is not reachable. */
class ReportSendIntentHashUseCase(
    private val sendIntentRepository: SendIntentRepository,
) {

    suspend operator fun invoke(intentId: String, txHash: String): Either<Throwable, Unit> {
        return sendIntentRepository.reportSendIntentHash(intentId = intentId, txHash = txHash)
    }
}