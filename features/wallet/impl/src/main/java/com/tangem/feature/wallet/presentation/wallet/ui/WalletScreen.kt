package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.transactions.Transaction
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletTopBar
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.TransactionsBlockGroupTitle
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.TransactionsBlockTitle
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.WalletManageButtons
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.WalletMarketplaceBlock
import com.tangem.feature.wallet.presentation.wallet.ui.decorations.walletContentItemDecoration
import com.tangem.feature.wallet.presentation.wallet.ui.utils.changeWalletAnimator

/**
 * Wallet screen
 *
 * @param state screen state
 *
* [REDACTED_AUTHOR]
 */
@Suppress("LongMethod")
@Composable
internal fun WalletScreen(state: WalletStateHolder) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        val walletsListState = rememberLazyListState()
        val changeableItemModifier = Modifier.changeWalletAnimator(walletsListState)

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues = scaffoldPaddings)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = TangemTheme.dimens.spacing8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                WalletsList(
                    config = state.walletsListConfig,
                    lazyListState = walletsListState,
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing14),
                )
            }

            if (state is WalletStateHolder.SingleCurrencyContent) {
                item {
                    WalletManageButtons(
                        buttons = state.buttons,
                        modifier = changeableItemModifier.padding(bottom = TangemTheme.dimens.spacing14),
                    )
                }
            }

            items(
                items = state.notifications,
                itemContent = { item ->
                    Notification(
                        state = item.state,
                        modifier = changeableItemModifier
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .padding(bottom = TangemTheme.dimens.spacing14),
                    )
                },
            )

            if (state is WalletStateHolder.SingleCurrencyContent) {
                item {
                    WalletMarketplaceBlock(
                        state = state.marketplaceBlockState,
                        modifier = changeableItemModifier.padding(horizontal = TangemTheme.dimens.spacing16),
                    )
                }
            }

            itemsIndexed(
                items = state.contentItems,
                key = { index, item ->
                    when (item) {
                        is WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle -> item.networkName
                        is WalletContentItemState.MultiCurrencyItem.Token -> item.state.id
                        is WalletContentItemState.SingleCurrencyItem.Title -> index
                        is WalletContentItemState.SingleCurrencyItem.GroupTitle -> item.title
                        is WalletContentItemState.SingleCurrencyItem.Transaction -> index
                    }
                },
                itemContent = { index, item ->
                    ContentItem(
                        item = item,
                        modifier = changeableItemModifier.walletContentItemDecoration(
                            currentIndex = index,
                            lastIndex = state.contentItems.lastIndex,
                        ),
                    )
                },
            )

            if (state is WalletStateHolder.MultiCurrencyContent) {
                item {
                    OrganizeTokensButton(
                        onClick = state.onOrganizeTokensClick,
                        modifier = changeableItemModifier
                            .padding(top = TangemTheme.dimens.spacing14)
                            .padding(horizontal = TangemTheme.dimens.spacing16),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentItem(item: WalletContentItemState, modifier: Modifier = Modifier) {
    when (item) {
        is WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle -> {
            NetworkGroupItem(networkName = item.networkName, modifier = modifier)
        }
        is WalletContentItemState.MultiCurrencyItem.Token -> {
            TokenItem(state = item.state, modifier = modifier)
        }
        is WalletContentItemState.SingleCurrencyItem.Title -> {
            TransactionsBlockTitle(config = item, modifier = modifier)
        }
        is WalletContentItemState.SingleCurrencyItem.GroupTitle -> {
            TransactionsBlockGroupTitle(config = item, modifier = modifier)
        }
        is WalletContentItemState.SingleCurrencyItem.Transaction -> {
            Transaction(state = item.state, modifier = modifier)
        }
    }
}

@Composable
private fun OrganizeTokensButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundedActionButton(
        config = ActionButtonConfig(
            text = stringResource(id = R.string.organize_tokens_title),
            iconResId = R.drawable.ic_filter_24,
            onClick = onClick,
        ),
        modifier = modifier,
    )
}

// region Preview
@Preview
@Composable
private fun WalletScreenPreview_Light(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme {
        WalletScreen(state)
    }
}

@Preview
@Composable
private fun WalletScreenPreview_Dark(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme(isDark = true) {
        WalletScreen(state)
    }
}

private class WalletScreenParameterProvider : CollectionPreviewParameterProvider<WalletStateHolder>(
    collection = listOf(
        WalletPreviewData.multicurrencyWalletScreenState,
        WalletPreviewData.singleWalletScreenState,
    ),
)
// endregion Preview
