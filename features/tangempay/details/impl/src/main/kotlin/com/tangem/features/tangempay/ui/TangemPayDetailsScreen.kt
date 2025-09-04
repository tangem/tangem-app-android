package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.features.tangempay.components.txHistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayDetailsScreen(
    state: TangemPayDetailsUM,
    txHistoryComponent: TangemPayTxHistoryComponent,
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
                item(
                    key = TangemPayCardDetailsUM::class.java,
                    content = {
                        TangemPayCardDetailsBlock(
                            modifier = modifier
                                .padding(top = TangemTheme.dimens.spacing12)
                                .padding(horizontal = TangemTheme.dimens.spacing16)
                                .fillMaxWidth(),
                            state = state.cardDetailsUM,
                        )
                    },
                )

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
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = "Tangem Pay",
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )
        FiatBalance(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            state = state,
            isBalanceHidden = isBalanceHidden,
        )
        CryptoBalance(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            state = state,
            isBalanceHidden = isBalanceHidden,
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
                textColor = TangemTheme.colors.text.primary1,
            ),
        )
        is TangemPayDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = DASH_SIGN.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

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

// region Card details block
@Composable
private fun TangemPayCardDetailsBlock(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            )
            .padding(all = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Card details",
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
            )
            Text(
                modifier = Modifier.clickable(onClick = state.onReveal),
                text = "Reveal",
                color = TangemTheme.colors.text.accent,
                style = TangemTheme.typography.body2,
            )
        }
        CardDetailsTextContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            text = state.number,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardDetailsTextContainer(modifier = Modifier.weight(1f), text = state.expiry)
            CardDetailsTextContainer(modifier = Modifier.weight(1f), text = state.cvv)
        }
    }
}

@Composable
private fun CardDetailsTextContainer(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(color = TangemTheme.colors.field.primary, shape = RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
            text = text,
            maxLines = 1,
            color = TangemTheme.colors.text.disabled,
            style = TangemTheme.typography.body2,
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
                            item = it,
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

@Preview(device = Devices.PIXEL_7_PRO, group = "day")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO, group = "night")
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
            ),
            cardDetailsUM = TangemPayCardDetailsUM(
                number = "•••• •••• •••• 1245",
                expiry = "••/••",
                cvv = "•••",
                onReveal = {},
            ),
            isBalanceHidden = false,
        ),
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = {}, items = null),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(actionButtons = persistentListOf()),
            cardDetailsUM = TangemPayCardDetailsUM(
                number = "•••• •••• •••• 1245",
                expiry = "••/••",
                cvv = "•••",
                onReveal = {},
            ),
            isBalanceHidden = false,
        ),
    ),
)