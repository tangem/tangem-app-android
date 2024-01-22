package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWalletId

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {

    /**
* [REDACTED_TODO_COMMENT]
     */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ) = Either.catch {
        requireNotNull(
            transactionRepository.createTransaction(
                amount = amount,
                fee = fee,
                memo = memo,
                destination = destination,
                userWalletId = userWalletId,
                network = network,
            ),
        ) { "Failed to create transaction" }
    }
}
