package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.PendingActionConstraints
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.staking.model.stakekit.*
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
internal sealed class InnerYieldBalanceState {
    data class Data(
        val integrationId: String?,
        val reward: YieldReward,
        val isActionable: Boolean,
        val balances: ImmutableList<BalanceState>,
    ) : InnerYieldBalanceState()

    data object Empty : InnerYieldBalanceState()
}

@Immutable
internal data class BalanceState(
    val groupId: String,
    val title: TextReference,
    val type: BalanceType,
    val subtitle: TextReference?,
    val isClickable: Boolean,
    val cryptoValue: String,
    val cryptoAmount: BigDecimal,
    val formattedCryptoAmount: TextReference,
    val fiatAmount: BigDecimal?,
    val formattedFiatAmount: TextReference,
    val rawCurrencyId: String?,
    val validator: Yield.Validator?,
    val pendingActions: ImmutableList<PendingAction>,
    val isPending: Boolean,
)

@Immutable
internal data class YieldReward(
    val rewardsCrypto: String,
    val rewardsFiat: String,
    val rewardBlockType: RewardBlockType,
    val rewardConstraints: PendingActionConstraints?,
)