package com.tangem.features.managetokens.choosetoken.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.features.managetokens.choosetoken.entity.ChooseManagedTokenUM
import com.tangem.features.managetokens.choosetoken.model.ChooseManagedTokensNotificationUM
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ChooseManagedTokenContent(state: ChooseManagedTokenUM, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.tertiary,
        contentWindowInsets = WindowInsetsZero,
        topBar = {
            ManageTokensTopBar(
                modifier = Modifier.statusBarsPadding(),
                topBar = state.readContent.topBar,
                search = state.readContent.search,
                focusRequester = focusRequester,
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                state = state,
            )
        },
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun Content(state: ChooseManagedTokenUM, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        Currencies(
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            notificationUM = state.notificationUM,
            items = state.readContent.items,
            showLoadingItem = state.readContent.isNextBatchLoading,
            onLoadMore = state.readContent.loadMore,
        )
    }

    EventEffect(event = state.readContent.scrollToTop) {
        listState.animateScrollToItem(index = 0)
    }
}

@Composable
private fun Currencies(
    listState: LazyListState,
    notificationUM: NotificationUM?,
    items: ImmutableList<CurrencyItemUM>,
    showLoadingItem: Boolean,
    onLoadMore: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) {
        WindowInsets.systemBars.getBottom(density = this).toDp()
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        state = listState,
        contentPadding = PaddingValues(
            bottom = TangemTheme.dimens.spacing76 + bottomBarHeight,
        ),
    ) {
        if (notificationUM?.config != null) {
            item(key = "notification_key") {
                Notification(
                    config = notificationUM.config,
                    iconTint = TangemTheme.colors.icon.accent,
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 12.dp),
                )
            }
        }
        contentItems(items)

        if (showLoadingItem) {
            item(key = "loading_item") {
                ProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                )
            }
        }
    }

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_ITEMS_BUFFER,
        onLoadMore = onLoadMore,
    )
}

private fun LazyListScope.contentItems(items: ImmutableList<CurrencyItemUM>) {
    itemsIndexed(
        items = items,
        key = { index, item -> item.id.value },
    ) { index, item ->
        when (item) {
            is CurrencyItemUM.Basic -> {
                ChainRow(
                    model = with(item) {
                        ChainRowUM(
                            name = name,
                            type = symbol,
                            icon = icon,
                            showCustom = false,
                        )
                    },
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            addDefaultPadding = false,
                            lastIndex = items.lastIndex,
                        )
                        .clickable(onClick = item.onExpandClick)
                        .background(TangemTheme.colors.background.action),
                )
            }
            is CurrencyItemUM.Loading -> {
                LoadingItem(
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            addDefaultPadding = false,
                            lastIndex = items.lastIndex,
                        )
                        .fillMaxWidth()
                        .background(TangemTheme.colors.background.action),
                )
            }
            is CurrencyItemUM.SearchNothingFound -> {
                SearchNothingFoundText(
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            addDefaultPadding = false,
                            lastIndex = items.lastIndex,
                        )
                        .background(TangemTheme.colors.background.action)
                        .fillParentMaxSize(),
                )
            }
            else -> Unit
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ChooseManagedTokenContent_Preview() {
    TangemThemePreview {
        ChooseManagedTokenContent(
            state = ChooseManagedTokenUM(
                notificationUM = ChooseManagedTokensNotificationUM.SendViaSwap({}),
                readContent = ManageTokensUM.ReadContent(
                    popBack = {},
                    isInitialBatchLoading = false,
                    isNextBatchLoading = false,
                    items = buildList {
                        repeat(10) {
                            add(
                                CurrencyItemUM.Basic(
                                    id = ManagedCryptoCurrency.ID(
                                        value = "ID+$it",
                                    ),
                                    name = "Bitcoin",
                                    symbol = "BTC",
                                    icon = CurrencyIconState.Loading,
                                    networks = CurrencyItemUM.Basic.NetworksUM.Collapsed,
                                    onExpandClick = {},
                                ),
                            )
                        }
                    }.toPersistentList(),
                    topBar = ManageTokensTopBarUM.ReadContent(
                        title = resourceReference(R.string.common_choose_token),
                        onBackButtonClick = {},
                    ),
                    search = SearchBarUM(
                        placeholderText = resourceReference(R.string.common_search),
                        query = "",
                        onQueryChange = {},
                        isActive = false,
                        onActiveChange = {},
                    ),
                    loadMore = { true },
                ),
            ),
        )
    }
}
// endregion