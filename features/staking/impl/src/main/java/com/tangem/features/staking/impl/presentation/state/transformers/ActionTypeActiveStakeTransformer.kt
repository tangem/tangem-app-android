package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.transformer.Transformer

internal class ActionTypeActiveStakeTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val activeStake: BalanceState,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val isTronStakedBalance = isTronStakedBalance(
            networkId = cryptoCurrencyStatus.currency.network.id,
            activeStake = activeStake,
        )
        val actionType = if (activeStake.pendingActions.isEmpty() || isTronStakedBalance) {
            StakingActionCommonType.EXIT
        } else {
            StakingActionCommonType.PENDING_OTHER
        }

        return prevState.copy(actionType = actionType)
    }

    private fun isTronStakedBalance(networkId: Network.ID, activeStake: BalanceState): Boolean {
        return isTron(networkId.value) && activeStake.type == BalanceType.STAKED
    }
}