package com.tangem.domain.onramp

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class GetOnrampTransactionUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(txId: String): Either<OnrampError, OnrampTransaction> {
        if (onrampTransactionRepository.isHandledTransaction(txId)) {
            return OnrampError.AlreadyHandledTransaction.left()
        }

        return Either.catch {
            requireNotNull(
                onrampTransactionRepository.getTransactionById(txId),
            )
        }.mapLeft(errorResolver::resolve)
    }
}