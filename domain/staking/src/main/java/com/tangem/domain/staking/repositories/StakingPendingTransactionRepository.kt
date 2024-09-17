package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.PendingTransaction

interface StakingPendingTransactionRepository {

    suspend fun getTransactions(): List<PendingTransaction>

    suspend fun saveTransaction(pendingTransaction: PendingTransaction)

    suspend fun removeTransaction(pendingTransaction: PendingTransaction)
}
