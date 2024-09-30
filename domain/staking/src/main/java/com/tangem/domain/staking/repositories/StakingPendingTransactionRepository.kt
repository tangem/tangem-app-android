package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.wallets.models.UserWalletId

interface StakingPendingTransactionRepository {

    fun getTransactionsWithBalanceItems(userWalletId: UserWalletId): List<Pair<PendingTransaction, BalanceItem>>

    fun saveTransaction(userWalletId: UserWalletId, transaction: PendingTransaction)

    fun removeTransactions(userWalletId: UserWalletId, transactions: Set<PendingTransaction>)
}