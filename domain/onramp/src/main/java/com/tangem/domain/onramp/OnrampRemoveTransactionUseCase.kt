package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampRemoveTransactionUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(txId: String): Either<OnrampError, Unit> {
        return Either.catch {
            onrampTransactionRepository.removeTransaction(txId)
        }.mapLeft {
            OnrampError.UnknownError
        }
    }
}