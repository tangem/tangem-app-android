package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.StakingStates

@Composable
internal fun StakingConfirmContent(state: StakingStates.ConfirmStakingState) {
    if (state !is StakingStates.ConfirmStakingState.Data) return

    Column(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(TangemTheme.dimens.spacing16)
            .verticalScroll(rememberScrollState()),
    ) {
        StakingFeeBlock(feeState = state.feeState)
    }
    // TODO staking
}
