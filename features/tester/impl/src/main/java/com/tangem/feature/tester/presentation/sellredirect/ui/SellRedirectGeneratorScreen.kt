package com.tangem.feature.tester.presentation.sellredirect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.sellredirect.state.SellRedirectGeneratorUM

/**
 * Screen listing `redirect_sell` deeplinks generated from the app's cached, app-initiated sells. Each item can be
 * copied to the clipboard or opened directly to route through the app's deeplink handling.
 *
 * @param state screen state
 */
@Composable
internal fun SellRedirectGeneratorScreen(state: SellRedirectGeneratorUM) {
    Scaffold(
        topBar = {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResourceSafe(id = R.string.sell_redirect_generator),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = TangemTheme.colors.background.secondary,
    ) { paddingValues ->
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item(key = "refresh") {
                    SecondaryButton(
                        text = "Refresh cached sells",
                        onClick = state.onRefreshClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (state.isEmpty) {
                    item(key = "empty") { EmptyMessage() }
                }

                items(items = state.items, key = { it.requestId + it.deepLink }) { item ->
                    Column(modifier = Modifier.animateItem()) {
                        DeepLinkItem(item = item)
                        DividerWithPadding(start = 16.dp, end = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage() {
    Text(
        text = "No cached sells found. Start a Sell (off-ramp) in the app to register one, then refresh here. " +
            "Records are single-use and expire after an hour.",
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.secondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun DeepLinkItem(item: SellRedirectGeneratorUM.DeepLinkItemUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Field(label = "Currency", value = item.currencyId)
        Field(label = "Wallet", value = item.walletId)
        Field(label = "Request id", value = item.requestId)
        Field(label = "Age", value = item.age)

        if (item.isExpired) {
            Text(
                text = "Expired — this deeplink is no longer valid",
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.warning,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text(
            text = item.deepLink,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                text = "Copy",
                onClick = item.onCopyClick,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Open",
                onClick = item.onOpenClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label:",
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
        )
        Text(
            text = value,
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}