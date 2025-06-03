package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository

class OnrampUpdateTransactionStatusUseCase(
    private val onrampTransactionRepository: OnrampTransactionRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(
        txId: String,
        externalTxId: String,
        externalTxUrl: String,
        status: OnrampStatus.Status,
    ) = Either.catch {
        onrampTransactionRepository.updateTransactionStatus(
            txId = txId,
            externalTxId = externalTxId,
            externalTxUrl = externalTxUrl,
            status = status,
        )
    }.mapLeft(errorResolver::resolve)
}