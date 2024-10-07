package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import com.tangem.domain.wallets.models.UserWalletId
import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.UUID

class InvalidatePendingTransactionsUseCase(
    private val stakingPendingTransactionRepository: StakingPendingTransactionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        balanceItems: List<BalanceItem>,
        balancesId: Int,
    ): Either<StakingError, List<BalanceItem>> {
        return Either.catch {
            val (balancesToDisplay, transactionsToRemove) = mergeRealAndPendingTransactions(
                realData = balanceItems,
                newBalancesId = balancesId,
                pendingData = stakingPendingTransactionRepository.getTransactionsWithBalanceItems(userWalletId),
            )

            stakingPendingTransactionRepository.removeTransactions(userWalletId, transactionsToRemove.toSet())

            balancesToDisplay
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }

    private fun mergeRealAndPendingTransactions(
        realData: List<BalanceItem>,
        newBalancesId: Int,
        pendingData: List<Pair<PendingTransaction, BalanceItem>>,
    ): Pair<List<BalanceItem>, List<PendingTransaction>> {
        val balances = realData.associateBy { BalanceIdentity(it.groupId, it.type, it.amount, it.date) }
            .toMutableMap()

        val transactionsToRemove = mutableListOf<PendingTransaction>()

        pendingData.forEach { (pendingTransaction, balanceItem) ->
            val key = BalanceIdentity(balanceItem.groupId, balanceItem.type, balanceItem.amount, balanceItem.date)
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
                    val now = DateTime.now()
                    balances[BalanceIdentity(groupId, BalanceType.STAKED, pendingTransaction.amount, now)] =
                        BalanceItem(
                            groupId = groupId,
                            type = pendingTransaction.type,
                            amount = pendingTransaction.amount,
                            rawCurrencyId = pendingTransaction.rawCurrencyId,
                            validatorAddress = pendingTransaction.validator?.address,
                            date = null,
                            pendingActions = emptyList(),
                            isPending = true,
                        )

                    balances.entries.find {
                        it.key.type == BalanceType.STAKED &&
                            it.key.amount == pendingTransaction.amount &&
                            !it.value.isPending
                    }
                        ?.key
                        ?.let { balances.remove(it) }
                }
            }
        }

        return balances.values.toList() to transactionsToRemove
    }

    private data class BalanceIdentity(
        val groupId: String,
        val type: BalanceType,
        val amount: BigDecimal,
        val date: DateTime?,
    )
}
