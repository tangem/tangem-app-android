package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmStakingStatePreviewData

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

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_StakingConfirmContent() {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.primary)) {
            StakingConfirmContent(state = ConfirmStakingStatePreviewData.confirmStakingState)
        }
    }
}



