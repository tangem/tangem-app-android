@file:OptIn(ExperimentalFoundationApi::class)

package com.tangem.tap.features.userWalletsList.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.features.userWalletsList.ui.Mock
import com.tangem.tap.features.userWalletsList.ui.WalletSelectorScreenState.UserWallet
import com.tangem.wallet.R

@Composable
internal fun WalletList(
    modifier: Modifier = Modifier,
    wallets: List<UserWallet>,
    fiatCurrency: FiatCurrency,
    selectedWalletId: String?,
    checkedWalletIds: List<String>,
    isLocked: Boolean,
    onWalletClick: (walletId: String) -> Unit,
    onWalletLongClick: (walletId: String) -> Unit,
) {
    val multiCurrencyWallets = remember(wallets) {
        wallets.filter { it.type is UserWallet.Type.MultiCurrency }
    }
    val singleCurrencyWallets = remember(wallets) {
        wallets.filter { it.type is UserWallet.Type.SingleCurrency }
    }
    LazyColumn(modifier = modifier) {
        val walletSection: (List<UserWallet>) -> Unit = { wallets ->
            itemsIndexed(
                items = wallets,
                key = { _, item -> item.id },
            ) { index, wallet ->
                if (index == 0) {
                    val textRes = when (wallet.type) {
                        is UserWallet.Type.MultiCurrency -> R.string.user_wallet_list_multi_header
                        is UserWallet.Type.SingleCurrency -> R.string.user_wallet_list_single_header
                    }
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = textRes),
                        color = colorResource(id = R.color.text_tertiary),
                        style = TangemTypography.subtitle2,
                    )
                }
                WalletItem(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onWalletClick(wallet.id) },
                            onLongClick = { onWalletLongClick(wallet.id) },
                        )
                        .padding(all = 16.dp),
                    wallet = wallet,
                    fiatCurrency = fiatCurrency,
                    isSelected = wallet.id == selectedWalletId,
                    isChecked = wallet.id in checkedWalletIds,
                    isLocked = isLocked && wallet.id != selectedWalletId,
                )
            }
        }

        walletSection(multiCurrencyWallets)
        walletSection(singleCurrencyWallets)
    }
}

// region Preview
@Composable
private fun WalletListSample(
    modifier: Modifier = Modifier,
) {
    val wallets = Mock.userWalletList
    Column(modifier = modifier) {
        WalletList(
            wallets = wallets,
            fiatCurrency = FiatCurrency.Default,
            selectedWalletId = wallets[0].id,
            checkedWalletIds = listOf(wallets[2].id),
            isLocked = false,
            onWalletClick = { /* no-op */ },
            onWalletLongClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletListPreview() {
    AppCompatTheme {
        WalletListSample()
    }
}
// endregion Preview
