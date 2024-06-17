package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmStakingStatePreviewData

@Composable
internal fun StakingConfirmContent(state: StakingStates.ConfirmStakingState) {
    if (state !is StakingStates.ConfirmStakingState.Data) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.tertiary)
            .padding(TangemTheme.dimens.spacing16)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        StakingFeeBlock(feeState = state.feeState)
        NotificationsBlock(notifications = state.notifications)

        SpacerHMax()

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.footerText,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NotificationsBlock(notifications: List<StakingNotification>) {
    notifications.forEach {
        Notification(config = it.config, iconTint = TangemTheme.colors.icon.accent)
    }
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
