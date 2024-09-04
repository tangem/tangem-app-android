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
import androidx.compose.ui.unit.Density
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.common.ui.navigationButtons.NavigationPrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.components.list.roundedListWithDividersItems
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.RewardBlockType
import com.tangem.features.staking.impl.R
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
private const val STAKE_PRIMARY_BUTTON_KEY = "StakePrimaryButton"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StakingInitialInfoContent(
    state: StakingStates.InitialInfoState,
    buttonState: NavigationButtonsState,
    clickIntents: StakingClickIntents,
) {
    if (state !is StakingStates.InitialInfoState.Data) return

    LazyColumn(
        verticalArrangement = alignLastToBottom(),
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        if (state.showBanner) {
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
                        rewardBlockType = state.yieldBalance.rewardBlockType,
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

        item(STAKE_PRIMARY_BUTTON_KEY) {
            StakeButtonBlock(buttonState)
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
    rewardBlockType: RewardBlockType,
    onRewardsClick: () -> Unit,
) {
    val (text, textColor) = when (rewardBlockType) {
        RewardBlockType.Rewards -> {
            annotatedReference {
                append(PLUS)
                appendSpace()
                append(rewardFiat)
                appendSpace()
                append(DOT)
                appendSpace()
                append(rewardCrypto)
            } to TangemTheme.colors.text.primary1
        }
        RewardBlockType.RewardUnavailable -> {
            resourceReference(R.string.staking_details_auto_claiming_rewards_daily_text) to
                TangemTheme.colors.text.tertiary
        }
        RewardBlockType.NoRewards -> {
            resourceReference(R.string.staking_details_no_rewards_to_claim) to TangemTheme.colors.text.tertiary
        }
    }
    InputRowDefault(
        title = resourceReference(R.string.staking_rewards),
        text = text,
        iconRes = R.drawable.ic_chevron_right_24.takeIf { rewardBlockType == RewardBlockType.Rewards },
        textColor = textColor,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                enabled = rewardBlockType == RewardBlockType.Rewards,
                onClick = onRewardsClick,
            ),
    )
}

@Composable
private fun ActiveStakingBlock(balances: ImmutableList<BalanceState>, onClick: (BalanceState) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    ) {
        Text(
            text = stringResource(id = R.string.staking_your_stakes),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(
                top = TangemTheme.dimens.spacing12,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing4,
            ),
        )
        balances.forEach { balance ->
            key(balance.id) {
                val (icon, iconTint) = balance.type.getIcon()
                InputRowImageInfo(
                    subtitle = balance.title,
                    caption = balance.subtitle ?: balance.getAprText(),
                    isGrayscaleImage = !balance.isClickable,
                    infoTitle = balance.fiatAmount,
                    infoSubtitle = balance.cryptoAmount,
                    imageUrl = balance.getImage(),
                    iconRes = icon,
                    iconTint = iconTint,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        enabled = balance.isClickable,
                        onClick = { onClick(balance) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun StakeButtonBlock(buttonState: NavigationButtonsState) {
    val state = buttonState as? NavigationButtonsState.Data
    val primaryButton = state?.primaryButton

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        state?.onTextClick?.let { StakingTosText(it) }
        NavigationPrimaryButton(primaryButton = primaryButton)
    }
}

@Composable
private fun BalanceState.getAprText() = combinedReference(
    resourceReference(R.string.staking_details_apr),
    annotatedReference {
        appendSpace()
        appendColored(
            text = BigDecimalFormatter.formatPercent(
                percent = validator?.apr.orZero(),
                useAbsoluteValue = true,
            ),
            color = TangemTheme.colors.text.accent,
        )
    },
)

@Composable
private fun BalanceType.getIcon() = when (this) {
    BalanceType.UNSTAKING -> R.drawable.ic_connection_18 to TangemTheme.colors.icon.accent
    BalanceType.UNSTAKED -> R.drawable.ic_connection_18 to TangemTheme.colors.icon.informative
    BalanceType.LOCKED -> R.drawable.ic_lock_24 to TangemTheme.colors.icon.informative
    else -> null to TangemTheme.colors.icon.informative
}

@Composable
private fun BalanceState.getImage() = when (type) {
    BalanceType.UNSTAKING,
    BalanceType.UNSTAKED,
    BalanceType.LOCKED,
    -> null
    else -> validator?.image
}

private val textGradientColors = listOf(
    TangemColorPalette.White,
    Color(0xff8fb4df),
)

@Composable
private fun alignLastToBottom() = remember {
    object : Arrangement.Vertical {
        override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            var currentOffset = 0

            sizes.forEachIndexed { index, size ->
                if (index == sizes.lastIndex) {
                    outPositions[index] = totalSize - size
                } else {
                    outPositions[index] = currentOffset
                    currentOffset += size
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
            buttonState = NavigationButtonsState.Empty,
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
