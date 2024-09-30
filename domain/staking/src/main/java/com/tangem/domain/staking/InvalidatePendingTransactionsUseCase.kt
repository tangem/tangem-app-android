package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import com.tangem.domain.wallets.models.UserWalletId
import java.util.UUID

class InvalidatePendingTransactionsUseCase(
    private val stakingPendingTransactionRepository: StakingPendingTransactionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        yieldBalance: YieldBalance,
    ): Either<StakingError, List<BalanceItem>> {
        return Either.catch {
            if (yieldBalance is YieldBalance.Data) {
                val (balancesToDisplay, transactionsToRemove) = mergeRealAndPendingTransactions(
                    yieldBalance = yieldBalance,
                    pending = stakingPendingTransactionRepository.getTransactionsWithBalanceItems(userWalletId),
                )

                stakingPendingTransactionRepository.removeTransactions(userWalletId, transactionsToRemove.toSet())

                balancesToDisplay
            } else {
                emptyList()
            }
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }

    private fun mergeRealAndPendingTransactions(
        yieldBalance: YieldBalance.Data,
        pending: List<Pair<PendingTransaction, BalanceItem>>,
    ): Pair<List<BalanceItem>, List<PendingTransaction>> {
        val balances = yieldBalance.balance.items.associateBy { Triple(it.groupId, it.type, it.amount) }.toMutableMap()
        val newBalancesId = yieldBalance.getBalancesUniqueId()

        val transactionsToRemove = mutableListOf<PendingTransaction>()

        pending.forEach { (pendingTransaction, balanceItem) ->
            val key = Triple(balanceItem.groupId, balanceItem.type, balanceItem.amount)
            val oldBalancesId = pendingTransaction.balancesId

            when {
                newBalancesId != oldBalancesId -> {
                    transactionsToRemove.add(pendingTransaction)
                }
                balances.containsKey(key) -> {
                    balances[key] = balanceItem
                }
                else -> {
                    val groupId = UUID.randomUUID().toString()
                    balances[Triple(groupId, BalanceType.STAKED, pendingTransaction.amount)] = BalanceItem(
                        groupId = groupId,
                        type = pendingTransaction.type,
                        amount = pendingTransaction.amount,
                        rawCurrencyId = pendingTransaction.rawCurrencyId,
                        validatorAddress = pendingTransaction.validator?.address,
                        date = null,
                        pendingActions = emptyList(),
                        isPending = true,
                    )
                }
            }
        }

        return balances.values.toList() to transactionsToRemove
    }
}
