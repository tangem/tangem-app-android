package com.tangem.feature.tester.presentation.walletids.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.presentation.walletids.state.WALLET_IDS_TITLE
import com.tangem.feature.tester.presentation.walletids.state.WalletIdsScreenUM
import com.tangem.feature.tester.presentation.walletids.state.WalletIdsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WalletIdsScreen(state: WalletIdsScreenUM) {
    Scaffold(
        topBar = {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = WALLET_IDS_TITLE,
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = TangemTheme.colors.background.secondary,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items = state.wallets, key = WalletIdsUM::id) { wallet ->
                WalletIdsItem(wallet = wallet)
            }
        }
    }
}

@Composable
private fun WalletIdsItem(wallet: WalletIdsUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = wallet.name,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        WalletIdRow(title = "id", value = wallet.id, onCopyClick = wallet.onCopyIdClick)
        WalletIdRow(title = "Amplitude WalletId hash", value = wallet.hash, onCopyClick = wallet.onCopyHashClick)
    }
}

@Composable
private fun WalletIdRow(title: String, value: String, onCopyClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = "$title: $value",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.secondary,
            )
        }
        IconButton(onClick = onCopyClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_copy_24),
                contentDescription = "Copy $title",
                tint = TangemTheme.colors.icon.primary1,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WalletIdsScreenPreview() {
    TangemThemePreview {
        WalletIdsScreen(
            state = WalletIdsScreenUM(
                wallets = persistentListOf(
                    WalletIdsUM(
                        name = "Wallet 1",
                        id = "0A1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3D4E5F60718293A4B5C6D7E8F9",
                        hash = "EC4916DD28FC4C10D78E287CA5D9CC51EE1AE73CBFDE08C6B37324CBFAAC8BC5",
                        onCopyIdClick = {},
                        onCopyHashClick = {},
                    ),
                ),
                onBackClick = {},
            ),
        )
    }
}