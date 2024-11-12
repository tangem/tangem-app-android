package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.utils.extensions.isEqualTo
import java.math.BigDecimal
import java.util.UUID

class InvalidatePendingTransactionsUseCase(
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        balanceItems: List<BalanceItem>,
        processingActions: List<StakingAction>,
    ): Either<StakingError, List<BalanceItem>> {
        return Either.catch {
            val balancesToDisplay = mergeBalancesAndProcessingActions(
                realBalances = balanceItems,
                processingActions = processingActions,
            )
            balancesToDisplay
        }.mapLeft {
            stakingErrorResolver.resolve(it)
        }
    }

    private fun mergeBalancesAndProcessingActions(
        realBalances: List<BalanceItem>,
        processingActions: List<StakingAction>,
    ): List<BalanceItem> {
        val balances = realBalances.toMutableList()

        processingActions.forEach { action ->
            when (action.type) {
                StakingActionType.STAKE, StakingActionType.VOTE -> {
                    addStubStakedPendingTransaction(balances, action)
                }
                StakingActionType.VOTE_LOCKED -> {
                    addStubStakedPendingTransaction(balances, action)
                    removeLockedBalance(balances, action)
                }
                StakingActionType.WITHDRAW -> {
                    modifyBalancesByStatus(balances, action, BalanceType.UNSTAKED)
                }
                StakingActionType.UNLOCK_LOCKED -> {
                    modifyBalancesByStatus(balances, action, BalanceType.LOCKED)
                }
                StakingActionType.RESTAKE -> {
                    modifyBalancesByStatus(balances, action, BalanceType.STAKED)
                }
                StakingActionType.UNSTAKE -> {
                    val isFullUnstake = modifyBalancesByStatus(balances, action, BalanceType.STAKED)
                    if (!isFullUnstake) {
                        processPartialUnstake(balances, action)
                    }
                }
                else -> {
                    // intentionally do nothing
                }
            }
        }

        return balances
    }

    private fun removeLockedBalance(balances: MutableList<BalanceItem>, action: StakingAction) {
        val index = findBalanceIndex(balances, action, BalanceType.LOCKED)

        if (index != -1) {
            balances.removeAt(index)
        }
    }

    private fun addStubStakedPendingTransaction(balances: MutableList<BalanceItem>, action: StakingAction) {
        balances.add(
            BalanceItem(
                groupId = UUID.randomUUID().toString(),
                token = balances[0].token,
                type = BalanceType.STAKED,
                amount = action.amount,
                rawCurrencyId = null,
                validatorAddress = action.validatorAddress ?: action.validatorAddresses?.get(0) ?: "",
                date = null,
                pendingActions = emptyList(),
                isPending = true,
            ),
        )
    }

    private fun modifyBalancesByStatus(
        balances: MutableList<BalanceItem>,
        action: StakingAction,
        type: BalanceType,
    ): Boolean {
        val index = findBalanceIndex(balances, action, type)

        if (index != -1) {
            balances[index] = balances[index].copy(isPending = true)
            return true
        }

        return false
    }

    private fun findBalanceIndex(balances: MutableList<BalanceItem>, action: StakingAction, type: BalanceType): Int {
        return balances.indexOfFirst {
            !it.isPending && it.amount isEqualTo action.amount && it.type == type
        }
    }

    private fun processPartialUnstake(balances: MutableList<BalanceItem>, action: StakingAction) {
        val (index, pendingActionAmount) = findPartialUnstake(balances, action)

        if (index != -1) {
            val amount = balances[index].amount
            balances[index] = balances[index].copy(
                amount = amount - pendingActionAmount,
            ) // remnants of real one

            balances.add(
                balances[index].copy(
                    amount = pendingActionAmount,
                    isPending = true,
                ),
            ) // pending with amount from action
        }
    }

    private fun findPartialUnstake(balances: MutableList<BalanceItem>, action: StakingAction): Pair<Int, BigDecimal> {
        val index = balances.indexOfFirst {
            !it.isPending && action.amount < it.amount &&
                it.type == BalanceType.STAKED &&
                it.validatorAddress == action.validatorAddress
        }
        return index to action.amount
    }
}
