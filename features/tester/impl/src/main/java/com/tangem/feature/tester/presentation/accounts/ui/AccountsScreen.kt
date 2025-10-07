package com.tangem.feature.tester.presentation.accounts.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.accounts.entity.AccountsUM

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AccountsScreen(state: AccountsUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    var isWalletSelectorShown by remember { mutableStateOf(false) }
    var isAccountListShown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        stickyHeader { AppBar(onBackClick = state.onBackClick) }

        item {
            WalletInfoBlock(
                walletName = state.walletSelector.selected?.name,
                onClick = {
                    if (state.walletSelector.wallets.isNotEmpty()) {
                        isWalletSelectorShown = true
                    }
                },
            )
        }

        if (state.walletSelector.selected != null) {
            ManageAccountsButtons(
                state = state,
                onAccountsClick = { context ->
                    val isEmpty = state.onAccountsClick()

                    if (!isEmpty) {
                        isAccountListShown = true
                    } else {
                        Toast.makeText(context, "No accounts found", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }

    WalletSelector(
        isShown = isWalletSelectorShown,
        onDismissRequest = { isWalletSelectorShown = false },
        content = state.walletSelector,
    )

    AccountList(
        isShown = isAccountListShown,
        onDismissRequest = { isAccountListShown = false },
        content = state.accountListBottomSheetConfig,
    )
}

@Composable
private fun AppBar(onBackClick: () -> Unit) {
    AppBarWithBackButton(
        onBackClick = onBackClick,
        text = stringResourceSafe(id = R.string.accounts),
        containerColor = TangemTheme.colors.background.primary,
    )
}

@Composable
private fun WalletInfoBlock(walletName: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(color = TangemTheme.colors.background.secondary, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_wallet_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )

        val text by remember(walletName) { mutableStateOf(walletName ?: "Choose wallet") }
        Text(
            text = text,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )

        Icon(
            painter = painterResource(R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.secondary,
        )
    }
}

@Composable
private fun WalletSelector(isShown: Boolean, onDismissRequest: () -> Unit, content: AccountsUM.WalletSelector) {
    TangemBottomSheet(
        config = TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = onDismissRequest,
            content = content,
        ),
        titleText = stringReference("Choose wallet"),
    ) { content: AccountsUM.WalletSelector ->
        content.wallets.fastForEachIndexed { index, wallet ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        content.onWalletSelect(wallet)
                        onDismissRequest()
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = wallet.name,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )

                if (wallet.walletId == content.selected?.walletId) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.accent,
                    )
                }
            }

            if (index == content.wallets.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                DividerWithPadding(start = 16.dp, end = 16.dp)
            }
        }
    }
}

@Composable
private fun AccountList(
    isShown: Boolean,
    onDismissRequest: () -> Unit,
    content: AccountsUM.AccountListBottomSheetConfig,
) {
    TangemBottomSheet(
        config = TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = onDismissRequest,
            content = content,
        ),
        titleText = stringReference("Accounts"),
    ) { content: AccountsUM.AccountListBottomSheetConfig ->
        content.accounts.fastForEachIndexed { index, account ->
            val accountName = account.accountName.toUM().value.resolveReference()
            Text(
                text = "#${account.derivationIndex.value} – $accountName",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )

            if (index == content.accounts.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                DividerWithPadding(start = 16.dp, end = 16.dp)
            }
        }
    }
}

@Suppress("FunctionNaming")
private fun LazyListScope.ManageAccountsButtons(state: AccountsUM, onAccountsClick: (Context) -> Unit) {
    item {
        val context = LocalContext.current

        PrimaryButton(
            text = stringResourceSafe(R.string.accounts),
            onClick = { onAccountsClick(context) },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        )
    }

    item {
        PrimaryButton(
            text = "Fetch accounts",
            onClick = state.onFetchAccountsClick,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        )
    }

    item {
        PrimaryButton(
            text = "Clear ETag",
            onClick = state.onClearETagClick,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        )
    }
}