package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.features.tangempay.components.cardDetails.PreviewTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.txHistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.*
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayDetailsScreen(
    state: TangemPayDetailsUM,
    txHistoryComponent: TangemPayTxHistoryComponent,
    cardDetailsBlockComponent: TangemPayCardDetailsBlockComponent,
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
        val cardDetailsState by cardDetailsBlockComponent.state.collectAsStateWithLifecycle()

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
                item(
                    key = TangemPayDetailsBalanceBlockState::class.java,
                    content = {
                        TangemPayDetailsBalanceBlock(
                            modifier = modifier
                                .padding(horizontal = TangemTheme.dimens.spacing16)
                                .fillMaxWidth(),
                            state = state.balanceBlockState,
                            isBalanceHidden = state.isBalanceHidden,
                        )
                    },
                )
                if (state.addToWalletBlockState != null) {
                    item(
                        key = AddToWalletBlockState::class.java,
                        content = {
                            TangemPayAddToWalletBlock(
                                state = state.addToWalletBlockState,
                                modifier = Modifier
                                    .padding(top = TangemTheme.dimens.spacing12)
                                    .padding(horizontal = TangemTheme.dimens.spacing16),
                            )
                        },
                    )
                }
                item(TangemPayCardDetailsUM::class.java) {
                    TangemPayCardDetailsBlockItem(component = cardDetailsBlockComponent, state = cardDetailsState)
                }
                when (val cardFrozenState = state.balanceBlockState.frozenState) {
                    is CardFrozenState.Frozen -> item(CardFrozenState.Frozen::class.java) {
                        PrimaryButton(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                            text = stringResourceSafe(R.string.tangempay_card_details_unfreeze_card),
                            onClick = cardFrozenState.onUnfreeze,
                        )
                    }
                    else -> Unit
                }
                with(txHistoryComponent) { txHistoryContent(listState = listState, state = txHistoryState) }
            }
        }
    }
}

// region Balance block
@Composable
internal fun TangemPayDetailsBalanceBlock(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResourceSafe(R.string.tangempay_title),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
            )
            TangemPayCardStatus(state = state.frozenState)
        }
        FiatBalance(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp),
            state = state,
            isBalanceHidden = isBalanceHidden,
        )
        // TODO [REDACTED_TASK_KEY]: Uncomment after adding crypto balance when the BFF is ready
        // CryptoBalance(
        //     modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        //     state = state,
        //     isBalanceHidden = isBalanceHidden,
        // )
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
private fun TangemPayCardStatus(state: CardFrozenState, modifier: Modifier = Modifier) {
    when (state) {
        is CardFrozenState.Unfrozen -> Unit
        is CardFrozenState.Pending -> CircularProgressIndicator(
            modifier = modifier.size(18.dp),
            trackColor = Color.Transparent,
            color = TangemColorPalette.Azure,
            strokeWidth = 2.dp,
        )
        is CardFrozenState.Frozen -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_snow_24),
                tint = TangemTheme.colors.text.disabled,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResourceSafe(R.string.tangem_pay_card_frozen),
                color = TangemTheme.colors.text.disabled,
                style = TangemTheme.typography.body2,
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
            modifier = modifier,
            text = state.fiatBalance.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.h2.applyBladeBrush(
                isEnabled = state.isBalanceFlickering,
                textColor = getTextColor(
                    isCardFrozen = state.frozenState is CardFrozenState.Frozen,
                    defaultColor = TangemTheme.colors.text.primary1,
                ),
            ),
        )
        is TangemPayDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = DASH_SIGN.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.h2,
            color = getTextColor(
                isCardFrozen = state.frozenState is CardFrozenState.Frozen,
                defaultColor = TangemTheme.colors.text.primary1,
            ),
        )
    }
}

@ReadOnlyComposable
@Composable
private fun getTextColor(isCardFrozen: Boolean, defaultColor: Color): Color {
    return if (isCardFrozen) TangemTheme.colors.text.tertiary else defaultColor
}

@Suppress("UnusedPrivateMember")
@Composable
private fun CryptoBalance(
    state: TangemPayDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemPayDetailsBalanceBlockState.Loading -> RectangleShimmer(
            modifier = modifier.size(
                width = TangemTheme.dimens.size70,
                height = TangemTheme.dimens.size16,
            ),
        )
        is TangemPayDetailsBalanceBlockState.Content -> Text(
            modifier = modifier,
            text = state.cryptoBalance.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.caption2.applyBladeBrush(
                isEnabled = state.isBalanceFlickering,
                textColor = TangemTheme.colors.text.tertiary,
            ),
        )
        is TangemPayDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = DASH_SIGN.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
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
            AnimatedVisibility(visible = config.items != null && config.items.isNotEmpty()) {
                IconButton(onClick = { showDropdownMenu = true }) {
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
                    config.items?.fastForEach {
                        TangemDropdownItem(
                            item = it.dropdownItem,
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
            cardDetailsBlockComponent = PreviewTangemPayCardDetailsBlockComponent(
                TangemPayCardDetailsUM(
                    number = "•••• •••• •••• 1245",
                    expiry = "••/••",
                    cvv = "•••",
                    onCopy = {},
                    onClick = {},
                    buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
                ),
            ),
        )
    }
}

private class TangemPayDetailsUMProvider : CollectionPreviewParameterProvider<TangemPayDetailsUM>(
    collection = listOf(
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = {}, items = null),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                actionButtons = persistentListOf(
                    ActionButtonConfig(
                        text = resourceReference(id = R.string.common_receive),
                        iconResId = R.drawable.ic_arrow_down_24,
                        onClick = {},
                    ),
                ),
                cryptoBalance = "1234.56 USDT",
                fiatBalance = "$1234.56",
                isBalanceFlickering = false,
                frozenState = CardFrozenState.Frozen(onUnfreeze = {}),
            ),
            addToWalletBlockState = AddToWalletBlockState({}, {}),
            isBalanceHidden = false,
            addFundsEnabled = true,
        ),
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = {}, items = null),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = persistentListOf(),
                frozenState = CardFrozenState.Pending,
            ),
            addToWalletBlockState = AddToWalletBlockState({}, {}),
            isBalanceHidden = false,
            addFundsEnabled = true,
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
            cardDetailsBlockComponent = PreviewTangemPayCardDetailsBlockComponent(
                TangemPayCardDetailsUM(
                    number = "•••• •••• •••• 1245",
                    expiry = "••/••",
                    cvv = "•••",
                    onCopy = {},
                    onClick = {},
                    buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
                ),
            ),
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