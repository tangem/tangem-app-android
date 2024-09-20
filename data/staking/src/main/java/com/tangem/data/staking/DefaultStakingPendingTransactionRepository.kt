package com.tangem.data.staking

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import java.util.concurrent.CopyOnWriteArrayList

internal class DefaultStakingPendingTransactionRepository : StakingPendingTransactionRepository {

    private val pendingTransactions = CopyOnWriteArrayList<PendingTransaction>()

    override fun saveTransaction(transaction: PendingTransaction) {
        pendingTransactions.add(transaction)
    }

    override fun removeTransactions(transactions: Set<PendingTransaction>) {
        pendingTransactions.removeAll(transactions)
    }

    override fun getTransactionsWithBalanceItems(): List<Pair<PendingTransaction, BalanceItem>> {
        return pendingTransactions.mapNotNull { pendingTransaction ->
            PendingTransactionItemConverter.convert(pendingTransaction)?.let { balanceItem ->
                pendingTransaction to balanceItem
            }
        }
    }
}