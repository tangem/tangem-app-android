package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampRemoveTransactionUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(txId: String?): Either<OnrampError, Unit> {
        if (txId == null) return OnrampError.DomainError("Transaction id not provided").left()

        return Either.catch {
            onrampTransactionRepository.removeTransaction(txId)
        }.mapLeft(errorResolver::resolve)
    }
}