package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.rows.CornersToRound
import com.tangem.core.ui.components.rows.RoundableCornersRow
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates

@Composable
internal fun StakingInitialInfoContent(state: StakingStates.InitialInfoState) {
    if (state !is StakingStates.InitialInfoState.Data) return

    Column(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(TangemTheme.dimens.spacing16)
            .verticalScroll(rememberScrollState()),
    ) {
        MetricsBlock(state)
        Spacer(modifier = Modifier.height(8.dp))
        StakingDetailsRows(state)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MetricsBlock(state: StakingStates.InitialInfoState.Data) {
    Column(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius12),
            )
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.staking_details_metrics_block_header),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = stringResource(id = R.string.staking_details_apr),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
                Text(
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                    text = state.aprRange.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.accent,
                )
            }
            Column(modifier = Modifier.weight(1F)) {
                Row {
                    Text(
                        modifier = Modifier.padding(end = TangemTheme.dimens.spacing4),
                        text = stringResource(id = R.string.staking_details_market_rating),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                    Icon(
                        modifier = Modifier
                            .size(TangemTheme.dimens.size16)
                            .align(Alignment.CenterVertically),
                        painter = painterResource(id = R.drawable.ic_alert_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.text.tertiary,
                    )
                }
                Text(
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                    text = "1", // TODO staking
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.accent,
                )
            }
        }
    }
}

@Composable
internal fun StakingDetailsRows(state: StakingStates.InitialInfoState.Data) {
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_available),
        endText = state.available,
        cornersToRound = CornersToRound.TOP_2,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_on_stake),
        endText = state.onStake,
        cornersToRound = CornersToRound.ZERO,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_apy),
        endText = state.aprRange.resolveReference(),
        cornersToRound = CornersToRound.ZERO,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_unbonding_period),
        endText = state.unbondingPeriod,
        cornersToRound = CornersToRound.ZERO,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_minimum_requirement),
        endText = state.minimumRequirement,
        cornersToRound = CornersToRound.ZERO,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_reward_claiming),
        endText = state.rewardClaiming,
        cornersToRound = CornersToRound.ZERO,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_warmup_period),
        endText = state.warmupPeriod,
        cornersToRound = CornersToRound.ZERO,
    )
    InitialInfoContentRow(
        startText = stringResource(id = R.string.staking_details_reward_schedule),
        endText = state.rewardSchedule,
        cornersToRound = CornersToRound.BOTTOM_2,
    )
}

@Composable
private fun InitialInfoContentRow(startText: String, endText: String, cornersToRound: CornersToRound) {
    RoundableCornersRow(
        startText = startText,
        startTextColor = TangemTheme.colors.text.primary1,
        startTextStyle = TangemTheme.typography.body2,
        endText = endText,
        endTextColor = TangemTheme.colors.text.tertiary,
        endTextStyle = TangemTheme.typography.body2,
        cornersToRound = cornersToRound,
        iconResId = null, // TODO staking add bottom sheets when text will be available
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingInitialInfoContent_Preview(
    @PreviewParameter(StakingInitialInfoContentPreviewProvider::class) feeState: StakingStates.InitialInfoState.Data,
) {
    TangemThemePreview {
        StakingInitialInfoContent(
            state = feeState,
        )
    }
}

private class StakingInitialInfoContentPreviewProvider : PreviewParameterProvider<StakingStates.InitialInfoState.Data> {
    override val values: Sequence<StakingStates.InitialInfoState.Data>
        get() = sequenceOf(
            StakingStates.InitialInfoState.Data(
                isPrimaryButtonEnabled = true,
                available = "15 SOL",
                onStake = "0 SOL",
                aprRange = stringReference("2.54-5.12%"),
                unbondingPeriod = "3d",
                minimumRequirement = "12 SOL",
                rewardClaiming = "Auto",
                warmupPeriod = "Days",
                rewardSchedule = "Block",
            ),
        )
}
// endregion