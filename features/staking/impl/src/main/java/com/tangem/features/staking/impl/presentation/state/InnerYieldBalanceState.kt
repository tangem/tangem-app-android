package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.RewardBlockType
import com.tangem.domain.staking.model.stakekit.Yield
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

internal sealed class InnerYieldBalanceState {
    data class Data(
        val rewardsCrypto: String,
        val rewardsFiat: String,
        val rewardBlockType: RewardBlockType,
        val balance: ImmutableList<BalanceState>,
    ) : InnerYieldBalanceState()

    data object Empty : InnerYieldBalanceState()
}

@Immutable
internal data class BalanceState(
    val id: String,
    val title: TextReference,
    val type: BalanceType,
    val subtitle: TextReference?,
    val isClickable: Boolean,
    val cryptoValue: String,
    val cryptoDecimal: BigDecimal,
    val cryptoAmount: TextReference,
    val fiatAmount: TextReference,
    val rawCurrencyId: String?,
    val validator: Yield.Validator?,
    val pendingActions: ImmutableList<PendingAction>,
)
