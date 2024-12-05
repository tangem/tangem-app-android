package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampRemoveTransactionUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(externalTxId: String?): Either<OnrampError, Unit> {
        if (externalTxId == null) return OnrampError.UnknownError.left()

        return Either.catch {
            onrampTransactionRepository.removeTransaction(externalTxId)
        }.mapLeft {
            OnrampError.UnknownError
        }
    }
}