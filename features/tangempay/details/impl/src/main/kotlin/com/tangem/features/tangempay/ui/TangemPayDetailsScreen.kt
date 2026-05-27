package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.core.ui.test.WalletConnectBottomSheetTestTags
import com.tangem.features.tangempay.components.express.PreviewEmptyExpressTransactionsComponent
import com.tangem.features.tangempay.components.txHistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun TangemPayDetailsScreen(
    state: TangemPayDetailsUM,
    txHistoryComponent: TangemPayTxHistoryComponent,
    expressTransactionsComponent: ExpressTransactionsComponent,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TangemPayDetailsTopAppBar(config = state.topBarConfig) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val listState = rememberLazyListState()
        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
        val txHistoryState by txHistoryComponent.state.collectAsStateWithLifecycle()
        val expressState by expressTransactionsComponent.state.collectAsStateWithLifecycle()
        val expressTransactionsBottomSheetState = expressState.bottomSheetSlot

        TangemPullToRefreshContainer(
            config = state.pullToRefreshConfig,
            modifier = Modifier.padding(scaffoldPaddings),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    bottom = TangemTheme.dimens.spacing16 + bottomBarHeight,
                ),
            ) {
                item(key = "title") {
                    TangemPayTitle(
                        modifier = Modifier
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .padding(top = 4.dp)
                            .fillMaxWidth(),
                    )
                }
                item(
                    key = TangemPayDetailsBalanceBlockState::class.java,
                    content = {
                        TangemPayDetailsBalanceBlock(
                            modifier = Modifier
                                .padding(horizontal = TangemTheme.dimens.spacing16)
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                            state = state.balanceBlockState,
                            isBalanceHidden = state.isBalanceHidden,
                        )
                        SpacerH12()
                    },
                )

                if (state.balanceBlockState.cardsBlockState.cards.fastAny { it.isReissuing }) {
                    item(
                        key = "REISSUE_MESSAGE",
                        content = {
                            TangemPayReplacingCardBlock(
                                modifier = Modifier
                                    .padding(horizontal = TangemTheme.dimens.spacing16)
                                    .fillMaxWidth(),
                            )
                            SpacerH12()
                        },
                    )
                } else {
                    if (state.addToWalletBlockState != null) {
                        item(
                            key = AddToWalletBlockState::class.java,
                            content = {
                                TangemPayAddToWalletBlock(
                                    state = state.addToWalletBlockState,
                                    modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                                )
                            },
                        )
                    }
                    if (state.accountDeactivatedNotificationConfig != null) {
                        item(
                            key = "DEACTIVATION_MESSAGE",
                            content = {
                                Notification(
                                    modifier = modifier
                                        .padding(horizontal = TangemTheme.dimens.spacing16)
                                        .fillMaxWidth(),
                                    config = state.accountDeactivatedNotificationConfig,
                                )
                                SpacerH12()
                            },
                        )
                    }
                }
                if (state.accountDeactivatedNotificationConfig == null) {
                    with(expressTransactionsComponent) {
                        expressTransactionsContent(
                            state = expressState.transactionsToDisplay,
                            modifier = modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                        )
                    }
                    with(txHistoryComponent) { txHistoryContent(listState = listState, state = txHistoryState) }
                }
            }
        }
        expressTransactionsBottomSheetState?.content()
    }
}

@Composable
private fun TangemPayTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResourceSafe(R.string.tangempay_payment_account),
            style = TangemTheme.typography.head,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
        )
        TangemPaySubtitle()
    }
}

@Suppress("MagicNumber")
@Composable
private fun TangemPaySubtitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.wrapContentWidth()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_polygon_22),
                contentDescription = null,
                tint = TangemTheme.colors.text.constantWhite,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing12)
                    .border(width = 2.dp, color = TangemTheme.colors.background.secondary, shape = CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8247E5))
                    .size(16.dp)
                    .testTag(WalletConnectBottomSheetTestTags.NETWORKS_ICONS),
            )
            Image(
                painter = painterResource(id = R.drawable.img_usdc_16),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .border(width = 2.dp, color = TangemTheme.colors.background.secondary, shape = CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .size(16.dp)
                    .testTag(WalletConnectBottomSheetTestTags.NETWORKS_ICONS),
            )
        }
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = stringResourceSafe(R.string.tangempay_usdc_on_polygon_network),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
        )
    }
}

// region Balance block
@Composable
private fun TangemPayDetailsBalanceBlock(
    state: TangemPayDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            )
            .padding(vertical = 12.dp),
    ) {
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = stringResourceSafe(R.string.common_balance_title),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )
        FiatBalance(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp),
            state = state,
            isBalanceHidden = isBalanceHidden,
        )
        CardsBlockRow(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            cardsBlockState = state.cardsBlockState,
        )
        if (state.actionButtons.isNotEmpty()) {
            HorizontalActionChips(
                modifier = Modifier.padding(top = 12.dp),
                buttons = state.actionButtons,
                containerColor = TangemTheme.colors.background.primary,
                contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun CardsBlockRow(
    cardsBlockState: TangemPayDetailsBalanceBlockState.CardsBlockState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val itemsModifier = Modifier.size(width = 48.dp, height = 32.dp)
        cardsBlockState.cards.fastForEach { card ->
            TangemPayCardItem(modifier = itemsModifier, card = card)
        }
        TangemIconButton(
            modifier = itemsModifier,
            onClick = cardsBlockState.onAddCardClick,
            iconRes = R.drawable.ic_plus_24,
            shape = RoundedCornerShape(4.dp),
        )
    }
}

@Composable
private fun TangemPayCardItem(card: TangemPayDetailsBalanceBlockState.Card, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = card.onClick),
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(
                if (card.isReissuing) {
                    R.drawable.img_visa_card_inactive_48_32
                } else {
                    R.drawable.img_visa_card_48_32
                },
            ),
            contentDescription = null,
        )
        if (!card.isReissuing) {
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp, bottom = 2.dp),
                text = card.lastDigits,
                style = TangemTheme.typography.overline.copy(letterSpacing = 0.sp),
                color = TangemTheme.colors.text.constantWhite,
            )
        }
    }
}

@Composable
private fun FiatBalance(
    state: TangemPayDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemPayDetailsBalanceBlockState.Loading -> RectangleShimmer(
            modifier = modifier.size(
                width = TangemTheme.dimens.size102,
                height = TangemTheme.dimens.size32,
            ),
        )
        is TangemPayDetailsBalanceBlockState.Content -> Text(
            modifier = modifier.testTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE),
            text = state.fiatBalance.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.h2.applyBladeBrush(
                isEnabled = state.isBalanceFlickering,
                textColor = TangemTheme.colors.text.primary1,
            ),
        )
        is TangemPayDetailsBalanceBlockState.Error -> Text(
            modifier = modifier.testTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE),
            text = DASH_SIGN.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}
// endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TangemPayDetailsTopAppBar(config: TangemPayDetailsTopBarConfig, modifier: Modifier = Modifier) {
    var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = config.onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "Back",
                )
            }
        },
        title = {},
        actions = {
            AnimatedVisibility(visible = config.items.isNotEmpty()) {
                IconButton(
                    onClick = {
                        config.onOpenMenu()
                        showDropdownMenu = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vertical_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = "More",
                        modifier = Modifier.testTag(TokenDetailsTopBarTestTags.MORE_BUTTON),
                    )
                }
            }

            TangemDropdownMenu(
                expanded = showDropdownMenu,
                modifier = Modifier.background(TangemTheme.colors.background.primary),
                onDismissRequest = { showDropdownMenu = false },
                content = {
                    config.items.fastForEach { item ->
                        TangemDropdownItem(
                            item = item,
                            dismissParent = { showDropdownMenu = false },
                        )
                    }
                },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    )
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayDetailsScreenPreview(
    @PreviewParameter(TangemPayDetailsUMProvider::class) state: TangemPayDetailsUM,
) {
    TangemThemePreview {
        TangemPayDetailsScreen(
            state = state,
            txHistoryComponent = PreviewTangemPayTxHistoryComponent(
                txHistoryUM = PreviewTangemPayTxHistoryComponent.contentUM,
            ),
            expressTransactionsComponent = PreviewEmptyExpressTransactionsComponent(),
        )
    }
}

private class TangemPayDetailsUMProvider : CollectionPreviewParameterProvider<TangemPayDetailsUM>(
    collection = listOf(
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = {}, onOpenMenu = {}, items = persistentListOf()),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                actionButtons = persistentListOf(
                    ActionButtonConfig(
                        text = resourceReference(id = R.string.common_receive),
                        iconResId = R.drawable.ic_arrow_down_24,
                        onClick = {},
                    ),
                ),
                fiatBalance = "$1234.56",
                isBalanceFlickering = false,
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(lastDigits = "1234", onClick = {}, isReissuing = false),
                        TangemPayDetailsBalanceBlockState.Card(lastDigits = "3456", onClick = {}, isReissuing = false),
                    ),
                    onAddCardClick = {},
                ),
            ),
            isBalanceHidden = false,
            addFundsEnabled = true,
            addToWalletBlockState = AddToWalletBlockState(onClick = {}, onClickClose = {}),
            accountDeactivatedNotificationConfig = null,
        ),
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = {}, onOpenMenu = {}, items = persistentListOf()),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = persistentListOf(),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = "1234",
                            onClick = {},
                            isReissuing = true,
                        ),
                    ),
                    onAddCardClick = {},
                ),
            ),
            isBalanceHidden = false,
            addFundsEnabled = true,
            addToWalletBlockState = null,
            accountDeactivatedNotificationConfig = null,
        ),
    ),
)

@Preview(device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayDetailsTxHistoryScreenPreview(
    @PreviewParameter(TangemPayDetailsTxHistoryProvider::class) state: TangemPayTxHistoryUM,
) {
    TangemThemePreview {
        TangemPayDetailsScreen(
            state = TangemPayDetailsUMProvider().values.first(),
            txHistoryComponent = PreviewTangemPayTxHistoryComponent(txHistoryUM = state),
            expressTransactionsComponent = PreviewEmptyExpressTransactionsComponent(),
        )
    }
}

private class TangemPayDetailsTxHistoryProvider : CollectionPreviewParameterProvider<TangemPayTxHistoryUM>(
    collection = listOf(
        PreviewTangemPayTxHistoryComponent.loadingUM,
        PreviewTangemPayTxHistoryComponent.contentUM,
        PreviewTangemPayTxHistoryComponent.emptyUM,
        PreviewTangemPayTxHistoryComponent.errorUM,
    ),
)