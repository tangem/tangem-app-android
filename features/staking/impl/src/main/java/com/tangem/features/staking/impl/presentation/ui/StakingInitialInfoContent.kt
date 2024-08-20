package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.components.list.roundedListWithDividersItems
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceGroupedState
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.previewdata.InitialStakingStatePreview
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.StringsSigns.PLUS
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList

private const val BANNER_BLOCK_KEY = "BannerBlock"
private const val STAKING_REWARD_BLOCK_KEY = "StakingRewardBlock"
private const val ACTIVE_STAKING_BLOCK_KEY = "ActiveStakingBlock"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StakingInitialInfoContent(state: StakingStates.InitialInfoState, clickIntents: StakingClickIntents) {
    if (state !is StakingStates.InitialInfoState.Data) return

    LazyColumn(
        modifier = Modifier
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        if (state.yieldBalance == InnerYieldBalanceState.Empty) {
            item(key = BANNER_BLOCK_KEY) {
                Column(
                    modifier = Modifier.animateItemPlacement(),
                ) {
                    BannerBlock(onClick = clickIntents::onInitialInfoBannerClick)
                    SpacerH12()
                }
            }
        }

        this.roundedListWithDividersItems(
            rows = state.infoItems,
            footerContent = { SpacerH12() },
        )

        if (state.yieldBalance is InnerYieldBalanceState.Data) {
            item(key = STAKING_REWARD_BLOCK_KEY) {
                Column(modifier = Modifier.animateItemPlacement()) {
                    StakingRewardBlock(
                        rewardCrypto = state.yieldBalance.rewardsCrypto,
                        rewardFiat = state.yieldBalance.rewardsFiat,
                        isRewardsToClaim = state.yieldBalance.isRewardsToClaim,
                        isRewardsClaimable = state.yieldBalance.isRewardsClaimable,
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
private fun BannerBlock(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
            ),
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            painter = painterResource(R.drawable.img_staking_banner),
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(TangemTheme.dimens.spacing16),
            text = buildAnnotatedString {
                withStyle(SpanStyle(Brush.linearGradient(textGradientColors))) {
                    append(stringResource(R.string.staking_details_banner_text))
                }
            },
            style = TangemTheme.typography.h2,
        )
    }
}

@Composable
private fun StakingRewardBlock(
    rewardCrypto: String,
    rewardFiat: String,
    isRewardsToClaim: Boolean,
    isRewardsClaimable: Boolean,
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
        iconRes = R.drawable.ic_chevron_right_24.takeIf { isRewardsToClaim && isRewardsClaimable },
        textColor = textColor,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                enabled = isRewardsToClaim && isRewardsClaimable,
                onClick = onRewardsClick,
            ),
    )
}

@Composable
private fun ActiveStakingBlock(groups: ImmutableList<BalanceGroupedState>, onClick: (BalanceState) -> Unit) {
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
                        Text(
                            text = group.title.resolveReference(),
                            style = TangemTheme.typography.subtitle2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(
                                top = TangemTheme.dimens.spacing12,
                                start = TangemTheme.dimens.spacing12,
                                end = TangemTheme.dimens.spacing12,
                            ),
                        )
                        group.items.forEach { balance ->
                            key(balance.validator.address) {
                                InputRowImageInfo(
                                    subtitle = stringReference(balance.validator.name),
                                    caption = getCaption(group.type, balance),
                                    isGrayscaleImage = group.type == BalanceType.UNSTAKING,
                                    infoTitle = balance.fiatAmount,
                                    infoSubtitle = balance.cryptoAmount,
                                    imageUrl = balance.validator.image.orEmpty(),
                                    iconEndRes = R.drawable.ic_chevron_right_24.takeIf { group.isClickable },
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

@Composable
private fun getCaption(balanceType: BalanceType, balance: BalanceState): TextReference {
    return if (balanceType == BalanceType.UNSTAKING) {
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
}

private val textGradientColors = listOf(
    TangemColorPalette.White,
    Color(0xff8fb4df),
)

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
