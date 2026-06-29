package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.components.topFade
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TextShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.BaseActionButtonsBlockTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.components.express.PreviewEmptyExpressTransactionsComponent
import com.tangem.features.tangempay.components.txHistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tangempay.ui.components.PayContextMenuBlock
import com.tangem.features.tangempay.ui.components.TangemPayActionButton
import com.tangem.features.tangempay.ui.components.TangemPayAddCardView
import com.tangem.features.tangempay.ui.components.TangemPayCardView
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.ImmutableList
import com.tangem.core.ui.R as CoreUiR

private val InitialTopBarHeight: Dp = 64.dp
private const val TOP_FADE_MID_STOP = 0.8f
private const val TOP_FADE_MID_ALPHA = 0.8f

@Suppress("LongMethod")
@Composable
internal fun TangemPayDetailsScreenV2(
    state: TangemPayDetailsUM,
    txHistoryComponent: TangemPayTxHistoryComponent,
    expressTransactionsComponent: ExpressTransactionsComponent,
    promoBannersBlockComponent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.systemBars.getTop(this).toDp() }
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    var topBarTotalHeight by remember { mutableStateOf(InitialTopBarHeight + statusBarHeight) }
    val rootBackground = TangemTheme.colors3.bg.primary

    val txHistoryState by txHistoryComponent.state.collectAsStateWithLifecycle()
    val expressState by expressTransactionsComponent.state.collectAsStateWithLifecycle()
    val expressTransactionsBottomSheetState = expressState.bottomSheetSlot

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(rootBackground),
    ) {
        TangemPullToRefreshSlidingContainer(
            config = state.pullToRefreshConfig,
            indicatorOffset = topBarTotalHeight,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .topFade(
                        height = topBarTotalHeight,
                        0f to rootBackground,
                        TOP_FADE_MID_STOP to rootBackground.copy(alpha = TOP_FADE_MID_ALPHA),
                        1f to Color.Transparent,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState,
                contentPadding = PaddingValues(
                    top = topBarTotalHeight,
                    bottom = TangemTheme.dimens2.x4 + bottomBarHeight,
                ),
            ) {
                payDetailsBody(state)
                item("promoBannersBlock") {
                    promoBannersBlockComponent.Content(
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                with(expressTransactionsComponent) {
                    expressTransactionsContent(
                        state = expressState.transactionsToDisplay,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    )
                }
                with(txHistoryComponent) { txHistoryContent(listState = listState, state = txHistoryState) }
            }
        }

        PayDetailsTopBar(
            config = state.topBarConfig,
            onHeightChange = { measuredHeight ->
                if (topBarTotalHeight != measuredHeight) topBarTotalHeight = measuredHeight
            },
        )
    }
    expressTransactionsBottomSheetState?.content(null)
}

@Suppress("LongMethod")
private fun LazyListScope.payDetailsBody(state: TangemPayDetailsUM) {
    item("balanceBlock") {
        BalanceBlock(
            state = state.balanceBlockState,
            isBalanceHidden = state.isBalanceHidden,
        )
    }
    state.balanceBlockState.cardsBlockState?.let { cardsState ->
        item("cardsBlock") {
            SpacerH12()
            CardsBlock(cardsBlockState = cardsState)
        }
    }
    if (state.balanceBlockState.actionButtons.isNotEmpty()) {
        item("actionButtonsBlock") {
            SpacerH24()
            ActionBlock(actionButtons = state.balanceBlockState.actionButtons)
        }
    }

    if (state.errorNotificationConfig != null) {
        item("errorSessionBannerBlock") {
            SpacerH12()
            ErrorMessage(
                config = state.errorNotificationConfig,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
            )
        }
    }

    val progressBanner = state.balanceBlockState.cardsBlockState?.progressBanner

    when (progressBanner) {
        CardsProgressBannerUM.Reissuing -> {
            item("reissuingBannerBlock") {
                SpacerH12()
                TangemMessage(
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
                    title = resourceReference(R.string.tangempay_reissue_card_in_progress),
                    subtitle = resourceReference(R.string.tangempay_reissue_card_in_progress_description),
                )
            }
        }
        CardsProgressBannerUM.Issuing -> {
            item("issuingBannerBlock") {
                SpacerH12()
                TangemMessage(
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
                    title = resourceReference(R.string.tangempay_issuing_new_digital_card_title),
                    subtitle = resourceReference(R.string.tangempay_reissue_card_in_progress_description),
                    contentColor = TangemTheme.colors3.bg.opaque.secondary,
                    leadingContent = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
                            contentDescription = null,
                            tint = TangemTheme.colors3.icon.primary,
                        )
                    },
                )
            }
        }
        null -> {
            if (state.addToWalletBlockState != null) {
                item("addToWalletBannerBlock") {
                    SpacerH12()
                    TangemPayAddToWalletBlock(
                        state = state.addToWalletBlockState,
                        modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
                    )
                }
            }
            if (state.accountDeactivatedNotificationConfig != null) {
                item("deactivationBannerBlock") {
                    SpacerH12()
                    ErrorMessage(
                        config = state.accountDeactivatedNotificationConfig,
                        modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(config: NotificationConfig, modifier: Modifier = Modifier) {
    val button = config.buttonsState
    TangemMessage(
        modifier = modifier,
        title = config.title,
        subtitle = config.subtitle,
        trailingContent = config.iconResId.takeIf { it != 0 }?.let { iconRes ->
            {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
            }
        },
        buttons = (button as? NotificationConfig.ButtonsState.SecondaryButtonConfig)?.let {
            {
                val iconResId = button.iconResId
                TangemButton(
                    buttonUM = TangemButtonUM(
                        text = button.text,
                        onClick = button.onClick,
                        iconPosition = TangemButtonIconPosition.End,
                        tangemIconUM = if (iconResId != null) {
                            TangemIconUM.Icon(
                                iconRes = iconResId,
                                tintReference = { TangemTheme.colors2.graphic.neutral.primaryInverted },
                            )
                        } else {
                            null
                        },
                        size = TangemButtonSize.X9,
                        type = TangemButtonType.Primary,
                        shape = TangemButtonShape.Rounded,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        messageEffect = TangemMessageEffect.Warning,
    )
}

@Composable
private fun PayDetailsTopBar(
    config: TangemPayDetailsTopBarConfig,
    onHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    TangemTopBar(
        modifier = modifier
            .onSizeChanged { size ->
                onHeightChange(with(density) { size.height.toDp() })
            }
            .statusBarsPadding(),
        title = resourceReference(R.string.tangempay_payment_account),
        subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
        startContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_arrow_back_28),
                onClick = config.onBackClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
        endContent = if (config.itemsV2.isNotEmpty()) {
            {
                var isDropdownMenuShown by rememberSaveable { mutableStateOf(false) }
                Box {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_more_default_24),
                        onClick = {
                            config.onOpenMenu()
                            isDropdownMenuShown = true
                        },
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                    PayContextMenuBlock(
                        items = config.itemsV2,
                        onMenuDismiss = { isDropdownMenuShown = false },
                        isDropdownMenuShown = isDropdownMenuShown,
                    )
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun BalanceBlock(
    state: TangemPayDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(top = TangemTheme.dimens2.x12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        AnimatedContent(
            targetState = state,
            label = "Updating the balance",
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 90))
            },
        ) { animatedState ->
            when (animatedState) {
                is TangemPayDetailsBalanceBlockState.Loading -> TextShimmer(
                    modifier = Modifier.size(width = 160.dp, height = 56.dp),
                    text = "1234.00",
                    style = TextShimmerStyle.HEADING_MEDIUM,
                    radius = TangemTheme.dimens2.x25,
                )
                is TangemPayDetailsBalanceBlockState.Content -> Text(
                    modifier = Modifier.testTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE),
                    text = animatedState.fiatBalance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
                    style = TangemTheme.typography3.display.medium.applyBladeBrush(
                        isEnabled = animatedState.isBalanceFlickering,
                        textColor = TangemTheme.colors3.text.primary,
                    ),
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography3.heading.medium.fontSize,
                        maxFontSize = TangemTheme.typography3.display.medium.fontSize,
                    ),
                )
                is TangemPayDetailsBalanceBlockState.Error -> Text(
                    modifier = Modifier.testTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE),
                    text = DASH_SIGN.orMaskWithStars(isBalanceHidden),
                    style = TangemTheme.typography3.display.medium,
                    color = TangemTheme.colors3.text.primary,
                )
            }
        }

        Text(
            modifier = Modifier.padding(vertical = TangemTheme.dimens2.x1),
            text = stringResourceSafe(R.string.token_details_balance_total),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
    }
}

@Composable
private fun CardsBlock(
    cardsBlockState: TangemPayDetailsBalanceBlockState.CardsBlockState,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = rememberLazyListState(),
        contentPadding = PaddingValues(
            horizontal = TangemTheme.dimens2.x4,
            vertical = TangemTheme.dimens2.x3,
        ),
        horizontalArrangement = Arrangement.Center,
    ) {
        items(items = cardsBlockState.cards) { item ->
            TangemPayCardView(
                isIssueInProgress = item.state != TangemPayCardUiState.Active,
                lastDigits = item.lastDigits,
                onClick = item.onClick,
                isEnabled = item.isEnabled,
                isFrozen = item.isFrozen,
            )
            SpacerW(TangemTheme.dimens2.x2)
        }
        item {
            TangemPayAddCardView(
                onClick = cardsBlockState.onAddCardClick,
                isEnabled = cardsBlockState.isAddCardEnabled,
            )
        }
    }
}

@Composable
private fun LazyItemScope.ActionBlock(
    actionButtons: ImmutableList<TangemPayActionButtonUM>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens2.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        actionButtons.fastForEach { actionButton ->
            val config = actionButton.config
            TangemPayActionButton(
                modifier = Modifier.testTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON),
                iconRes = config.iconResId,
                onClick = config.onClick,
                isEnabled = config.isEnabled,
                isLoading = config.isInProgress,
                title = config.text,
            )
        }
    }
}

// region preview

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayDetailsScreenPreview(
    @PreviewParameter(TangemPayDetailsUMProvider::class) state: TangemPayDetailsUM,
) {
    TangemThemePreviewRedesign {
        TangemPayDetailsScreenV2(
            state = state,
            txHistoryComponent = PreviewTangemPayTxHistoryComponent(
                txHistoryUM = PreviewTangemPayTxHistoryComponent.contentUM,
            ),
            expressTransactionsComponent = PreviewEmptyExpressTransactionsComponent(),
            promoBannersBlockComponent = ComposableContentComponent.EMPTY,
        )
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayDetailsTxHistoryScreenPreview(
    @PreviewParameter(TangemPayDetailsTxHistoryProvider::class) state: TangemPayTxHistoryUM,
) {
    TangemThemePreviewRedesign {
        TangemPayDetailsScreenV2(
            state = TangemPayDetailsUMProvider().values.first(),
            txHistoryComponent = PreviewTangemPayTxHistoryComponent(txHistoryUM = state),
            expressTransactionsComponent = PreviewEmptyExpressTransactionsComponent(),
            promoBannersBlockComponent = ComposableContentComponent.EMPTY,
        )
    }
}

// end region preview