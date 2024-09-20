package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository

class InvalidatePendingTransactionsUseCase(
    private val stakingPendingTransactionRepository: StakingPendingTransactionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(yieldBalance: YieldBalance): Either<StakingError, List<BalanceItem>> {
        return Either.catch {
            if (yieldBalance is YieldBalance.Data) {
                val (balancesToDisplay, transactionsToRemove) = mergeRealAndPendingTransactions(
                    real = yieldBalance.balance.items,
                    pending = stakingPendingTransactionRepository.getTransactionsWithBalanceItems(),
                )

                stakingPendingTransactionRepository.removeTransactions(transactionsToRemove.toSet())

                balancesToDisplay
            } else {
                emptyList()
            }
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }

    private fun mergeRealAndPendingTransactions(
        real: List<BalanceItem>,
        pending: List<Pair<PendingTransaction, BalanceItem>>,
    ): Pair<List<BalanceItem>, List<PendingTransaction>> {
        val map = real.associateBy { Triple(it.groupId, it.type, it.amount) }.toMutableMap()

        val toRemove = mutableListOf<PendingTransaction>()

        pending.forEach { (pendingTransaction, balanceItem) ->
            val key = Triple(balanceItem.groupId, balanceItem.type, balanceItem.amount)
            if (map.containsKey(key)) {
                map[key] = balanceItem
            } else {
                toRemove.add(pendingTransaction)
            }
        }

        return map.values.toList() to toRemove
    }
}