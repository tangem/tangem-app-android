package com.tangem.domain.staking

import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.repositories.StakingErrorResolver
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
                StakingActionType.STAKE, StakingActionType.VOTE, StakingActionType.VOTE_LOCKED -> {
                    processEnterAction(balances, action)
                }
                StakingActionType.WITHDRAW -> {
                    modifyBalancesByStatus(balances, action, BalanceType.UNSTAKED)
                }
                StakingActionType.UNLOCK_LOCKED -> {
                    modifyBalancesByStatus(balances, action, BalanceType.LOCKED)
                }
                StakingActionType.UNSTAKE -> {
                    modifyBalancesByStatus(balances, action, BalanceType.STAKED)
                }
                else -> {
                    // intentionally do nothing
                }
            }
        }

        return balances
    }

    private fun processEnterAction(balances: MutableList<BalanceItem>, action: StakingAction) {
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

    private fun modifyBalancesByStatus(balances: MutableList<BalanceItem>, action: StakingAction, type: BalanceType) {
        val index = balances.indexOfFirst {
            !it.isPending && it.amount == action.amount && it.type == type
        }

        if (index != -1) {
            balances[index] = balances[index].copy(isPending = true)
        }
    }
}