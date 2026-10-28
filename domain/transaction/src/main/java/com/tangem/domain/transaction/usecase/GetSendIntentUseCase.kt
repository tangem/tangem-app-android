package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.transaction.SendIntentRepository
import com.tangem.domain.transaction.models.SendIntent

class GetSendIntentUseCase(
    private val sendIntentRepository: SendIntentRepository,
) {

    suspend operator fun invoke(intentId: String): Either<Throwable, SendIntent> {
        return sendIntentRepository.getSendIntent(intentId = intentId)
    }
}