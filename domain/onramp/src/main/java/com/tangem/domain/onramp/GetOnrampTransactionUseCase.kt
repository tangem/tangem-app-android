package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class GetOnrampTransactionUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(externalTxId: String): Either<OnrampError, OnrampTransaction> {
        return Either.catch {
            requireNotNull(
                onrampTransactionRepository.getTransactionById(externalTxId),
            )
        }.mapLeft {
            OnrampError.UnknownError
        }
    }
}