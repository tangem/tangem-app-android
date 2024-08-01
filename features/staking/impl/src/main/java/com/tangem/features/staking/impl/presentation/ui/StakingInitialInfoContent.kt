package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.components.list.roundedListItems
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.previewdata.InitialStakingStatePreview
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.extensions.orZero

private const val STAKING_REWARD_BLOCK_KEY = "StakingRewardBlock"
private const val METRICS_BLOCK_KEY = "MetricsBlock"
private const val ACTIVE_STAKING_BLOCK_KEY = "ActiveStakingBlock"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StakingInitialInfoContent(state: StakingStates.InitialInfoState, clickIntents: StakingClickIntents) {
    if (state !is StakingStates.InitialInfoState.Data) return

    LazyColumn(
        modifier = Modifier
            .background(TangemTheme.colors.background.tertiary)
            .fillMaxSize(),
        contentPadding = PaddingValues(TangemTheme.dimens.spacing16),
    ) {
        if (state.yieldBalance == InnerYieldBalanceState.Empty) {
            item(key = METRICS_BLOCK_KEY) {
                Column(modifier = Modifier.animateItemPlacement()) {
                    MetricsBlock(state)
                    SpacerH12()
                }
            }
        }

        this.roundedListItems(state.infoItems)
        item { SpacerH12() }

        if (state.yieldBalance is InnerYieldBalanceState.Data) {
            item(key = STAKING_REWARD_BLOCK_KEY) {
                Column(modifier = Modifier.animateItemPlacement()) {
                    StakingRewardBlock(
                        rewardCrypto = state.yieldBalance.rewardsCrypto,
                        rewardFiat = state.yieldBalance.rewardsFiat,
                        isRewardsToClaim = state.yieldBalance.isRewardsToClaim,
                        onRewardsClick = clickIntents::openRewardsValidators,
                    )
                    SpacerH12()
                }
            }
        }

        if (state.yieldBalance is InnerYieldBalanceState.Data) {
            item(key = ACTIVE_STAKING_BLOCK_KEY) {
                Column(modifier = Modifier.animateItemPlacement()) {
                    ActiveStakingBlock(state.yieldBalance.balance, clickIntents::onActiveStake)
                    SpacerH12()
                }
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
private fun StakingRewardBlock(
    rewardCrypto: String,
    rewardFiat: String,
    isRewardsToClaim: Boolean,
    onRewardsClick: () -> Unit,
) {
    val (text, textColor) = if (isRewardsToClaim) {
        annotatedReference {
            append(PLUS)
            appendSpace()
            append(rewardFiat)
            appendSpace()
            append(DOT)
            appendSpace()
            append(rewardCrypto)
        } to TangemTheme.colors.text.primary1
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
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                enabled = isRewardsToClaim,
                onClick = onRewardsClick,
            ),
    )
}

@Composable
private fun ActiveStakingBlock(groups: List<BalanceGroupedState>, onClick: (BalanceState) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        groups.forEach { group ->
            key(group.title) {
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
                            key(balance.validator.address) {
                                val caption = if (group.type == BalanceType.UNSTAKING) {
                                    combinedReference(
                                        resourceReference(R.string.staking_details_unbonding_period),
                                        annotatedReference {
                                            appendSpace()
                                            appendColored(
                                                text = balance.unbondingPeriod.resolveReference(),
                                                color = TangemTheme.colors.text.accent,
                                            )
                                        },
                                    )
                                } else {
                                    combinedReference(
                                        resourceReference(R.string.app_name),
                                        annotatedReference {
                                            appendSpace()
                                            appendColored(
                                                text = BigDecimalFormatter.formatPercent(
                                                    percent = balance.validator.apr.orZero(),
                                                    useAbsoluteValue = true,
                                                ),
                                                color = TangemTheme.colors.text.accent,
                                            )
                                        },
                                    )
                                }
                                InputRowImageInfo(
                                    title = group.title.takeIf { index == 0 },
                                    subtitle = stringReference(balance.validator.name),
                                    caption = caption,
                                    isGrayscaleImage = group.type == BalanceType.UNSTAKING,
                                    infoTitle = balance.fiatAmount,
                                    infoSubtitle = balance.cryptoAmount,
                                    imageUrl = balance.validator.image.orEmpty(),
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberRipple(),
                                        enabled = group.isClickable,
                                        onClick = { onClick(balance) },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// region preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingInitialInfoContent_Preview(
    @PreviewParameter(StakingInitialInfoContentPreviewProvider::class) feeState: StakingStates.InitialInfoState.Data,
) {
    TangemThemePreview {
        StakingInitialInfoContent(
            state = feeState,
            clickIntents = StakingClickIntentsStub,
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