package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case to create and get transaction
 *
 * !!!IMPORTANT
 * Use when transaction data is already compiled by external service or provider
 */
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
        txExtras: TransactionExtras? = null,
    ) = Either.catch {
        transactionRepository.createTransaction(
            amount = amount,
            fee = fee,
            memo = memo,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
            txExtras = txExtras,
        )
    }
}