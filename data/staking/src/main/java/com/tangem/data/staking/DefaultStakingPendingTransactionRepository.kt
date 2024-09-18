package com.tangem.data.staking

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import java.util.concurrent.CopyOnWriteArrayList

internal class DefaultStakingPendingTransactionRepository : StakingPendingTransactionRepository {

    private val pendingTransactions = CopyOnWriteArrayList<PendingTransaction>()

    override fun saveTransaction(pendingTransaction: PendingTransaction) {
        pendingTransactions.add(pendingTransaction)
    }

    override fun removeTransaction(pendingTransaction: PendingTransaction) {
        pendingTransactions.remove(pendingTransaction)
    }

    override fun getTransactions(): List<PendingTransaction> {
        return pendingTransactions
    }
}
