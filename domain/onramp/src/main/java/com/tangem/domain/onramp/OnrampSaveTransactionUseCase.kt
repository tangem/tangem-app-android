package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampSaveTransactionUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(transaction: OnrampTransaction): Either<OnrampError, Unit> {
        return Either.catch { onrampTransactionRepository.storeTransaction(transaction) }
            .mapLeft { OnrampError.UnknownError }
    }
}