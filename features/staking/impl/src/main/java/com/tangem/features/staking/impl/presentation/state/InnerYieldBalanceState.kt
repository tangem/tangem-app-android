package com.tangem.features.staking.impl.presentation.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.Yield

sealed class InnerYieldBalanceState {
    data class Data(
        val rewardsCrypto: String,
        val rewardsFiat: String,
        val isRewardsToClaim: Boolean,
        val balance: List<BalanceGroupedState>,
    ) : InnerYieldBalanceState()

    data object Empty : InnerYieldBalanceState()
}

data class BalanceGroupedState(
    val items: List<BalanceState>,
    val type: BalanceType,
    val footer: TextReference?,
    val title: TextReference,
)

data class BalanceState(
    val validator: Yield.Validator,
    val cryptoAmount: TextReference,
    val fiatAmount: TextReference,
    val rawCurrencyId: String?,
)