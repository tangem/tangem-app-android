package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.transactions.Transaction
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletManageButtons
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletCardsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletTopBar
import com.tangem.feature.wallet.presentation.wallet.ui.decorations.walletContentItemDecoration

/**
 * Wallet screen
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletScreen(state: WalletStateHolder, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        LazyColumn(
            modifier = modifier
                .padding(paddingValues = scaffoldPaddings)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing8,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                WalletCardsList(
                    wallets = state.wallets,
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                )
            }

            if (state is WalletStateHolder.SingleCurrencyContent) {
                item { WalletManageButtons(buttons = state.buttons) }
            }

            items(items = state.notifications, itemContent = { Notification(state = it.state) })

            itemsIndexed(
                items = state.contentItems,
                key = { index, item ->
                    when (item) {
                        is WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle -> item.networkName
                        is WalletContentItemState.MultiCurrencyItem.Token -> item.state.id
                        is WalletContentItemState.SingleCurrencyItem.Title -> index
                        is WalletContentItemState.SingleCurrencyItem.TransactionGroupTitle -> item.title
                        is WalletContentItemState.SingleCurrencyItem.Transaction -> index
                    }
                },
                itemContent = { index, item ->
                    ContentItem(currentIndex = index, lastIndex = state.contentItems.lastIndex, item = item)
                },
            )

            if (state is WalletStateHolder.MultiCurrencyContent) {
                item {
                    RoundedActionButton(
                        config = ActionConfig(
                            text = stringResource(id = R.string.organize_tokens_title),
                            iconResId = R.drawable.ic_filter_24,
                            onClick = state.onOrganizeTokensClick,
                        ),
                        modifier = Modifier.padding(top = TangemTheme.dimens.spacing14),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentItem(currentIndex: Int, lastIndex: Int, item: WalletContentItemState) {
    val itemModifier = Modifier.walletContentItemDecoration(currentIndex, lastIndex)

    when (item) {
        is WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle -> {
            NetworkGroupItem(networkName = item.networkName, modifier = itemModifier)
        }
        is WalletContentItemState.MultiCurrencyItem.Token -> {
            TokenItem(state = item.state, modifier = itemModifier)
        }
        is WalletContentItemState.SingleCurrencyItem.Title -> {
            SingleCurrencyTitle(config = item, modifier = itemModifier)
        }
        is WalletContentItemState.SingleCurrencyItem.TransactionGroupTitle -> {
            TransactionGroupTitle(config = item, modifier = itemModifier)
        }
        is WalletContentItemState.SingleCurrencyItem.Transaction -> {
            Transaction(state = item.state, modifier = itemModifier)
        }
    }
}

@Composable
private fun SingleCurrencyTitle(
    config: WalletContentItemState.SingleCurrencyItem.Title,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens.spacing12)
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.common_transactions),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )

        Row(
            modifier = Modifier.clickable(onClick = config.onExploreClick),
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing4),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_compass_24),
                contentDescription = null,
                modifier = Modifier.size(size = TangemTheme.dimens.size18),
                tint = TangemTheme.colors.icon.informative,
            )
            Text(
                text = stringResource(id = R.string.common_explorer),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
            )
        }
    }
}

@Composable
private fun TransactionGroupTitle(
    config: WalletContentItemState.SingleCurrencyItem.TransactionGroupTitle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = config.title,
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing14,
            ),
        color = TangemTheme.colors.text.tertiary,
        textAlign = TextAlign.Start,
        style = TangemTheme.typography.body2,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletScreenPreview_Light(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme {
        WalletScreen(state)
    }
}

@Preview(showBackground = true, widthDp = 360)
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