package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.models.TransactionType
import java.math.BigInteger

class CreateTransactionDataExtrasUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(
        data: String,
        network: Network,
        transactionType: TransactionType,
        gasLimit: BigInteger? = null,
        nonce: BigInteger? = null,
    ) = Either.catch {
        requireNotNull(
            transactionRepository.createTransactionDataExtras(
                data = data,
                network = network,
                transactionType = transactionType,
                nonce = nonce,
                gasLimit = gasLimit,
            ),
        ) { "Failed to create transaction" }
    }
}