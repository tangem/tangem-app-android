package com.tangem.data.staking

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import com.tangem.domain.wallets.models.UserWalletId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal class DefaultStakingPendingTransactionRepository : StakingPendingTransactionRepository {

    private val pendingTransactionsMap = ConcurrentHashMap<UserWalletId, CopyOnWriteArrayList<PendingTransaction>>()

    override fun saveTransaction(userWalletId: UserWalletId, transaction: PendingTransaction) {
        val transactions = pendingTransactionsMap.computeIfAbsent(userWalletId) { CopyOnWriteArrayList() }
        transactions.add(transaction)
    }

    override fun removeTransactions(userWalletId: UserWalletId, transactions: Set<PendingTransaction>) {
        pendingTransactionsMap[userWalletId]?.removeAll(transactions)
    }

    override fun getTransactionsWithBalanceItems(
        userWalletId: UserWalletId,
    ): List<Pair<PendingTransaction, BalanceItem>> {
        return pendingTransactionsMap[userWalletId]?.mapNotNull { pendingTransaction: PendingTransaction ->
            PendingTransactionItemConverter.convert(pendingTransaction)?.let { balanceItem ->
                pendingTransaction to balanceItem
            }
        } ?: emptyList()
    }
}