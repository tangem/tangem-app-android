package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem

interface StakingPendingTransactionRepository {

    fun getTransactionsWithBalanceItems(): List<Pair<PendingTransaction, BalanceItem>>

    fun saveTransaction(transaction: PendingTransaction)

    fun removeTransactions(transactions: Set<PendingTransaction>)
}