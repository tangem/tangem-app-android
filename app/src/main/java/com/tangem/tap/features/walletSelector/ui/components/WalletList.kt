@file:OptIn(ExperimentalFoundationApi::class)

package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

@Composable
internal fun WalletList(
    modifier: Modifier = Modifier,
    wallets: List<UserWalletItem>,
    selectedWalletId: String?,
    checkedWalletIds: List<String>,
    isLocked: Boolean,
    onWalletClick: (walletId: String) -> Unit,
    onWalletLongClick: (walletId: String) -> Unit,
) {
    val multiCurrencyWallets = remember(wallets) {
        wallets.filter { it.type is UserWalletItem.Type.MultiCurrency }
    }
    val singleCurrencyWallets = remember(wallets) {
        wallets.filter { it.type is UserWalletItem.Type.SingleCurrency }
    }
    LazyColumn(modifier = modifier) {
        val walletSection: (List<UserWalletItem>) -> Unit = { wallets ->
            itemsIndexed(
                items = wallets,
                key = { _, item -> item.id },
            ) { index, wallet ->
                if (index == 0) {
                    Text(
                        modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
                        text = wallet.type.headerText.resolveReference(),
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
                WalletItem(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onWalletClick(wallet.id) },
                            onLongClick = { onWalletLongClick(wallet.id) },
                        )
                        .padding(all = TangemTheme.dimens.spacing16),
                    wallet = wallet,
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