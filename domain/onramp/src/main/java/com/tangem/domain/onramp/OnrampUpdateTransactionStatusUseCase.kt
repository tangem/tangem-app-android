package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampUpdateTransactionStatusUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(externalTxId: String, externalTxUrl: String, status: OnrampStatus.Status) =
        Either.catch {
            onrampTransactionRepository.updateTransactionStatus(
                externalTxId = externalTxId,
                externalTxUrl = externalTxUrl,
                status = status,
            )
        }.mapLeft(errorResolver::resolve)
}