package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import java.math.BigDecimal

@Immutable
internal sealed interface StakingBlockUM {

    data object TemporaryUnavailable : StakingBlockUM

    data class Loading(val iconState: IconState) : StakingBlockUM

    data class Staked(
        val cryptoValue: TextReference,
        val fiatValue: TextReference,
        val rewardValue: TextReference,
        val cryptoAmount: BigDecimal?,
        val fiatAmount: BigDecimal?,
        val onStakeClicked: () -> Unit,
    ) : StakingBlockUM

    data class StakeAvailable(
        val iconState: IconState,
        val titleText: TextReference,
        val subtitleText: TextReference,
        val isEnabled: Boolean,
        val onStakeClicked: () -> Unit,
    ) : StakingBlockUM
}