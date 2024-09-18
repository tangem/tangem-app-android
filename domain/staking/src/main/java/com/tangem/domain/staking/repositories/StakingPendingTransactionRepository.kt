package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.PendingTransaction

interface StakingPendingTransactionRepository {

    fun getTransactions(): List<PendingTransaction>

    fun saveTransaction(pendingTransaction: PendingTransaction)

    fun removeTransaction(pendingTransaction: PendingTransaction)
}
