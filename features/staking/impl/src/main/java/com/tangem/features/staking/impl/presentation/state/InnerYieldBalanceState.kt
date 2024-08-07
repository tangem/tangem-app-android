package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.Yield
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

sealed class InnerYieldBalanceState {
    data class Data(
        val rewardsCrypto: String,
        val rewardsFiat: String,
        val isRewardsToClaim: Boolean,
        val balance: ImmutableList<BalanceGroupedState>,
    ) : InnerYieldBalanceState()

    data object Empty : InnerYieldBalanceState()
}

// TODO staking get rid of unstable types
@Immutable
data class BalanceGroupedState(
    val items: ImmutableList<BalanceState>,
    val footer: TextReference?,
    val title: TextReference,
    val type: BalanceType,
    val isClickable: Boolean,
)

@Immutable
data class BalanceState(
    val validator: Yield.Validator,
    val cryptoValue: String,
    val cryptoDecimal: BigDecimal,
    val cryptoAmount: TextReference,
    val fiatAmount: TextReference,
    val rawCurrencyId: String?,
    val unbondingPeriod: TextReference,
    val pendingActions: ImmutableList<PendingAction>,
)