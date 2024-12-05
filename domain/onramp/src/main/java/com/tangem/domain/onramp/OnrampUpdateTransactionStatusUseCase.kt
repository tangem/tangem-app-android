package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampUpdateTransactionStatusUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(txId: String, status: OnrampStatus.Status) = Either.catch {
        onrampTransactionRepository.updateTransactionStatus(txId = txId, status = status)
    }
}