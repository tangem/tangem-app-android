package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.transformer.Transformer

internal class ActionTypeActiveStakeTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val activeStake: BalanceState,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val isTron = isTron(cryptoCurrencyStatus.currency.network.id.value)
        val actionType = if (activeStake.pendingActions.isEmpty() || isTron) {
            StakingActionCommonType.EXIT
        } else {
            StakingActionCommonType.PENDING_OTHER
        }

        return prevState.copy(actionType = actionType)
    }
}
