package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.components.rows.CornersToRound
import com.tangem.core.ui.components.rows.RoundableCornersRow
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceGroupedState
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.previewdata.InitialStakingStatePreview
import com.tangem.features.staking.impl.presentation.state.transformers.InfoType
import com.tangem.utils.Strings.DOT
import com.tangem.utils.extensions.orZero

@Composable
internal fun StakingInitialInfoContent(state: StakingStates.InitialInfoState) {
    if (state !is StakingStates.InitialInfoState.Data) return

    Column(
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            )
            .verticalScroll(rememberScrollState()),
    ) {
        AnimatedVisibility(state.yieldBalance == InnerYieldBalanceState.Empty) {
            MetricsBlock(state)
        }
        StakingDetailsRows(state)
        AnimatedContent(targetState = state.yieldBalance, label = "Rewards block visibility animation") {
            if (it is InnerYieldBalanceState.Data) {
                StakingRewardBlock(
                    rewardCrypto = it.rewardsCrypto,
                    rewardFiat = it.rewardsFiat,
                    isRewardsToClaim = it.isRewardsToClaim,
                )
            }
        }
        AnimatedContent(targetState = state.yieldBalance, label = "Rewards block visibility animation") {
            if (it is InnerYieldBalanceState.Data) {
                ActiveStakingBlock(it.balance)
            }
        }
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
            .padding(TangemTheme.dimens.spacing12)
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.staking_details_metrics_block_header),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        Spacer(modifier = Modifier.height(TangemTheme.dimens.size8))
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
    Column {
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
            iconClick = { state.onInfoClick(InfoType.APY) },
        )
        InitialInfoContentRow(
            startText = stringResource(id = R.string.staking_details_unbonding_period),
            endText = state.unbondingPeriod,
            cornersToRound = CornersToRound.ZERO,
            iconClick = { state.onInfoClick(InfoType.UNBOUNDING_PERIOD) },
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
            iconClick = { state.onInfoClick(InfoType.REWARD_CLAIMING) },
        )
        InitialInfoContentRow(
            startText = stringResource(id = R.string.staking_details_warmup_period),
            endText = state.warmupPeriod,
            cornersToRound = CornersToRound.ZERO,
            iconClick = { state.onInfoClick(InfoType.WARMUP_PERIOD) },
        )
        InitialInfoContentRow(
            startText = stringResource(id = R.string.staking_details_reward_schedule),
            endText = state.rewardSchedule,
            cornersToRound = CornersToRound.BOTTOM_2,
            iconClick = { state.onInfoClick(InfoType.REWARD_SCHEDULE) },
        )
    }
}

@Composable
private fun StakingRewardBlock(rewardCrypto: String, rewardFiat: String, isRewardsToClaim: Boolean) {
    val (text, textColor) = if (isRewardsToClaim) {
        annotatedReference(
            buildAnnotatedString {
                append("+")
                appendSpace()
                append(rewardFiat)
                appendSpace()
                append(DOT)
                appendSpace()
                append(rewardCrypto)
            },
        ) to TangemTheme.colors.text.primary1
    } else {
        resourceReference(R.string.staking_details_no_rewards_to_claim) to TangemTheme.colors.text.tertiary
    }
    InputRowDefault(
        title = resourceReference(R.string.staking_rewards),
        text = text,
        iconRes = R.drawable.ic_chevron_right_24,
        textColor = textColor,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    )
}

@Composable
private fun ActiveStakingBlock(groupes: List<BalanceGroupedState>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        groupes.forEach { group ->
            FooterContainer(
                footer = group.footer?.resolveReference(),
                modifier = Modifier,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .background(TangemTheme.colors.background.action),
                ) {
                    group.items.forEachIndexed { index, balance ->
                        InputRowImageInfo(
                            title = group.title.takeIf { index == 0 },
                            subtitle = stringReference(balance.validator.name),
                            caption = stringReference(
                                BigDecimalFormatter.formatPercent(balance.validator.apr.orZero(), true),
                            ),
                            infoTitle = balance.fiatAmount,
                            infoSubtitle = balance.cryptoAmount,
                            imageUrl = balance.validator.image.orEmpty(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialInfoContentRow(
    startText: String,
    endText: String,
    cornersToRound: CornersToRound,
    iconClick: (() -> Unit)? = null,
) {
    RoundableCornersRow(
        startText = startText,
        startTextColor = TangemTheme.colors.text.primary1,
        startTextStyle = TangemTheme.typography.body2,
        endText = endText,
        endTextColor = TangemTheme.colors.text.tertiary,
        endTextStyle = TangemTheme.typography.body2,
        cornersToRound = cornersToRound,
        iconResId = R.drawable.ic_information_24,
        iconClick = iconClick,
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
            InitialStakingStatePreview.defaultState,
            InitialStakingStatePreview.stateWithYield,
        )
}
// endregion
