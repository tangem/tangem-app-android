package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

internal data class StakingBlocksState(
    val stakingAvailable: StakingAvailable,
    val stakingBalance: StakingBalance,
)

@Immutable
internal sealed interface StakingAvailable {
    val iconState: IconState

    data class Error(override val iconState: IconState) : StakingAvailable

    data class Loading(override val iconState: IconState) : StakingAvailable

    data class Content(
        override val iconState: IconState,
        val interestRate: String,
        val periodInDays: Int,
        val tokenSymbol: String,
        val onStakeClicked: () -> Unit,
    ) : StakingAvailable
}

@Immutable
sealed class StakingBalance {
    data object Empty : StakingBalance()

    data class Content(
        val cryptoAmount: TextReference,
        val fiatAmount: TextReference,
        val rewardAmount: TextReference,
    ) : StakingBalance()
}