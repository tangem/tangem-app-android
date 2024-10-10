package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
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
import com.tangem.utils.extensions.orZero

private const val BANNER_BLOCK_KEY = "BannerBlock"
private const val STAKING_REWARD_BLOCK_KEY = "StakingRewardBlock"
private const val ACTIVE_STAKING_BLOCK_KEY = "ActiveStakingBlock"
private const val STAKE_PRIMARY_BUTTON_KEY = "StakePrimaryButton"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
internal fun StakingInitialInfoContent(
    state: StakingStates.InitialInfoState,
    buttonState: NavigationButtonsState,
    clickIntents: StakingClickIntents,
    isBalanceHidden: Boolean,
) {
    if (state !is StakingStates.InitialInfoState.Data) return

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.pullToRefreshConfig.isRefreshing,
        onRefresh = { state.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState()) },
    )
    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
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
                        modifier = Modifier.animateItem(),
                    ) {
                        BannerBlock(onClick = clickIntents::onInitialInfoBannerClick)
                        SpacerH12()
                    }
                }
            }

            this.roundedListWithDividersItems(
                rows = state.infoItems,
                footerContent = { SpacerH12() },
                hideEndText = isBalanceHidden,
            )

            activeStakingBlock(
                state = state,
                clickIntents = clickIntents,
                isBalanceHidden = isBalanceHidden,
            )

            item(STAKE_PRIMARY_BUTTON_KEY) {
                SpacerH12()
                StakeButtonBlock(buttonState)
            }
        }

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.pullToRefreshConfig.isRefreshing,
            state = pullRefreshState,
        )
    }
}

private fun LazyListScope.activeStakingBlock(
    state: StakingStates.InitialInfoState.Data,
    clickIntents: StakingClickIntents,
    isBalanceHidden: Boolean,
) {
    val balances = (state.yieldBalance as? InnerYieldBalanceState.Data)?.balance
    if (!balances.isNullOrEmpty()) {
        item(key = STAKING_REWARD_BLOCK_KEY) {
            Column(modifier = Modifier.animateItem()) {
                StakingRewardBlock(
                    yieldBalanceState = state.yieldBalance,
                    onRewardsClick = clickIntents::openRewardsValidators,
                    isBalanceHidden = isBalanceHidden,
                )
                SpacerH12()
            }
        }
        item(ACTIVE_STAKING_BLOCK_KEY) {
            Text(
                text = stringResource(id = R.string.staking_your_stakes),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = 0,
                        lastIndex = 1 + state.yieldBalance.balance.lastIndex,
                        addDefaultPadding = false,
                    )
                    .fillMaxWidth()
                    .background(TangemTheme.colors.background.action)
                    .padding(
                        top = TangemTheme.dimens.spacing12,
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                        bottom = TangemTheme.dimens.spacing4,
                    ),
            )
        }
        itemsIndexed(
            items = state.yieldBalance.balance,
            key = { _, balance ->
                // Staked balance does not have unique identifier.
                balance.toString()
            },
        ) { index, balance ->
            ActiveStakingBlock(
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                onClick = clickIntents::onActiveStake,
                onAnalytic = clickIntents::onActiveStakeAnalytic,
                modifier = Modifier
                    .animateItem()
                    .roundedShapeItemDecoration(
                        currentIndex = index + 1,
                        lastIndex = state.yieldBalance.balance.lastIndex + 1,
                        addDefaultPadding = false,
                    ),
            )
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
                indication = ripple(),
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
    yieldBalanceState: InnerYieldBalanceState.Data,
    onRewardsClick: () -> Unit,
    isBalanceHidden: Boolean,
) {
    val (text, textColor) = when (yieldBalanceState.rewardBlockType) {
        RewardBlockType.Rewards -> {
            annotatedReference {
                append(yieldBalanceState.rewardsFiat.orMaskWithStars(isBalanceHidden))
                appendSpace()
                append(DOT)
                appendSpace()
                append(yieldBalanceState.rewardsCrypto.orMaskWithStars(isBalanceHidden))
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
    val isShowIcon = yieldBalanceState.rewardBlockType == RewardBlockType.Rewards && yieldBalanceState.isActionable
    InputRowDefault(
        title = resourceReference(R.string.staking_rewards),
        text = text,
        iconRes = R.drawable.ic_chevron_right_24.takeIf { isShowIcon },
        textColor = textColor,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = yieldBalanceState.isActionable,
                onClick = onRewardsClick,
            ),
    )
}

@Composable
private fun ActiveStakingBlock(
    balance: BalanceState,
    isBalanceHidden: Boolean,
    onClick: (BalanceState) -> Unit,
    onAnalytic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, iconTint) = balance.type.getIcon()
    InputRowImageInfo(
        subtitle = balance.title,
        caption = balance.subtitle ?: balance.getAprText(),
        infoTitle = balance.fiatAmount.orMaskWithStars(isBalanceHidden),
        infoSubtitle = balance.cryptoAmount.orMaskWithStars(isBalanceHidden),
        imageUrl = balance.getImage(),
        iconRes = icon,
        iconTint = iconTint,
        subtitleEndIconRes = R.drawable.ic_staking_pending_transaction.takeIf { balance.isPending },
        onImageError = { ValidatorImagePlaceholder() },
        modifier = modifier
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = balance.isClickable,
                onClick = {
                    onAnalytic()
                    onClick(balance)
                },
            ),
    )
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
            isBalanceHidden = false,
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
