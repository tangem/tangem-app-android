package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampUpdateTransactionStatusUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
) {

    suspend operator fun invoke(externalTxId: String, status: OnrampStatus.Status) = Either.catch {
        onrampTransactionRepository.updateTransactionStatus(externalTxId = externalTxId, status = status)
    }
}