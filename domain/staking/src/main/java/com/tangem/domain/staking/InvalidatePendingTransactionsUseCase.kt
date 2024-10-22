package com.tangem.domain.staking

import android.util.Log
import arrow.core.Either
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.repositories.StakingActionRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

class InvalidatePendingTransactionsUseCase(
    private val stakingActionRepository: StakingActionRepository,
    private val stakingErrorResolver: StakingErrorResolver,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
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
        val balances = realBalances
            .associateBy { BalanceIdentity(it.groupId, it.amount) }
            .toMutableMap()

        processingActions.forEach { action ->
            // TODO staking
            val key = BalanceIdentity(action.id, action.amount)

            when (action.type) {
                StakingActionType.STAKE, StakingActionType.VOTE, StakingActionType.VOTE_LOCKED -> {
                }
                StakingActionType.WITHDRAW -> {
                }
                StakingActionType.UNLOCK_LOCKED -> {
                }
                StakingActionType.UNSTAKE -> {
                }
                else -> {
                    // intentionally do nothing
                }
            }
        }

        return balances.values.toList()
    }

    private data class BalanceIdentity(
        val groupId: String,
        val amount: BigDecimal,
    )
}
