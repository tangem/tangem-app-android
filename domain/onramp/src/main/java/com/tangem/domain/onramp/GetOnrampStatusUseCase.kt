package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampError
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class GetOnrampStatusUseCase(
    private val onrampRepository: OnrampRepository,
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(txId: String): Either<OnrampError, OnrampStatus> {
        return Either.catch {
            val status = onrampRepository.getStatus(txId)
            onrampTransactionRepository.updateTransactionStatus(txId = txId, status = status.status)
            status
        }.mapLeft {
            errorResolver.resolve(it)
        }
    }
}