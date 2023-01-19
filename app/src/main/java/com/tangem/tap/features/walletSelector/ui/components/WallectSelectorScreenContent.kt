package com.tangem.tap.features.walletSelector.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButtonIconRight
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.features.walletSelector.ui.WalletSelectorScreenState
import com.tangem.wallet.R

@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WalletSelectorScreenContent(
    state: WalletSelectorScreenState,
    onWalletClick: (UserWalletId) -> Unit,
    onWalletLongClick: (UserWalletId) -> Unit,
    onUnlockClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    LazyColumn {
        stickyHeader {
            Header(
                editingWalletsIds = state.editingUserWalletsIds,
                onClearSelectedClick = onClearSelectedClick,
                onEditSelectedWalletClick = onEditSelectedWalletClick,
                onDeleteSelectedWalletsClick = onDeleteSelectedWalletsClick,
            )
        }

        item {
            WalletsTitle(textResId = R.string.user_wallet_list_multi_header, wallets = state.multiCurrencyWallets)
        }

        itemsIndexed(
            items = state.multiCurrencyWallets,
            key = { _, wallet -> wallet.id.stringValue },
        ) { _, wallet ->

            WalletItem(
                wallet = wallet,
                isSelected = remember(state.selectedUserWalletId) { wallet.id == state.selectedUserWalletId },
                isChecked = remember(state.editingUserWalletsIds) { wallet.id in state.editingUserWalletsIds },
                onWalletClick = { onWalletClick(wallet.id) },
                onWalletLongClick = { onWalletLongClick(wallet.id) },
            )
        }

        item {
            WalletsTitle(textResId = R.string.user_wallet_list_single_header, wallets = state.singleCurrencyWallets)
        }

        itemsIndexed(
            items = state.singleCurrencyWallets,
            key = { _, wallet -> wallet.id.stringValue },
        ) { _, wallet ->
            WalletItem(
                wallet = wallet,
                isSelected = remember(state.selectedUserWalletId) { wallet.id == state.selectedUserWalletId },
                isChecked = remember(state.editingUserWalletsIds) { wallet.id in state.editingUserWalletsIds },
                onWalletClick = { onWalletClick(wallet.id) },
                onWalletLongClick = { onWalletLongClick(wallet.id) },
            )
        }

        item {
            Footer(
                isLocked = state.isLocked,
                showUnlockProgress = state.showUnlockProgress,
                showAddCardProgress = state.showAddCardProgress,
                onUnlockClick = onUnlockClick,
                onAddCardClick = onAddCardClick,
            )
        }
    }
}

@Composable
private fun Header(
    editingWalletsIds: List<UserWalletId>,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    val editingWalletsSize by rememberUpdatedState(newValue = editingWalletsIds.size)
    val hasEditingWallets by remember { derivedStateOf { editingWalletsSize > 0 } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.plain,
                shape = TangemTheme.shapes.bottomSheet,
            ),
    ) {
        Hand()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size44),
            contentAlignment = Alignment.Center,
        ) {
            if (hasEditingWallets) {
                EditWalletsBar(
                    editingWalletsSize = editingWalletsSize,
                    onClearSelectedClick = onClearSelectedClick,
                    onEditSelectedWalletClick = onEditSelectedWalletClick,
                    onDeleteSelectedWalletsClick = onDeleteSelectedWalletsClick,
                )
            } else {
                Text(
                    text = stringResource(R.string.user_wallet_list_title),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WalletsTitle(@StringRes textResId: Int, wallets: List<*>) {
    if (wallets.isNotEmpty()) {
        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
            text = stringResource(id = textResId),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun Footer(
    isLocked: Boolean,
    showUnlockProgress: Boolean,
    showAddCardProgress: Boolean,
    onUnlockClick: () -> Unit,
    onAddCardClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(
                top = dimensionResource(id = R.dimen.spacing24),
                bottom = dimensionResource(id = R.dimen.spacing16),
            )
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        if (isLocked) {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    id = R.string.user_wallet_list_unlock_all,
                    stringResource(id = R.string.common_biometrics),
                ),
                showProgress = showUnlockProgress,
                onClick = onUnlockClick,
            )
        }
        SecondaryButtonIconRight(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.user_wallet_list_add_button),
            showProgress = showAddCardProgress,
            icon = painterResource(id = R.drawable.ic_tangem_24),
            onClick = onAddCardClick,
        )
    }
}

@Composable
private fun EditWalletsBar(
    editingWalletsSize: Int,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    val showEditAction by remember(editingWalletsSize) {
        derivedStateOf { editingWalletsSize in 1 until 2 }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        IconButton(
            modifier = Modifier.size(TangemTheme.dimens.size32),
            onClick = onClearSelectedClick,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_close_24),
                tint = TangemTheme.colors.icon.primary1,
                contentDescription = "Unselect wallets",
            )
        }
        Text(
            text = stringResource(id = R.string.user_wallet_list_editing_count, editingWalletsSize),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()
        if (showEditAction) {
            IconButton(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                onClick = onEditSelectedWalletClick,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = R.drawable.ic_pencil_outline_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "Change wallet name",
                )
            }
        }
        IconButton(
            modifier = Modifier.size(TangemTheme.dimens.size32),
            onClick = onDeleteSelectedWalletsClick,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_trash),
                tint = TangemTheme.colors.icon.warning,
                contentDescription = "Delete selected wallets",
            )
        }
    }
}

// region Preview
@Composable
private fun WalletSelectorScreenContentSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.primary),
    ) {
        WalletSelectorScreenContent(
            state = MockData.state.copy(isLocked = true),
            onWalletClick = { /* no-op */ },
            onWalletLongClick = { /* no-op */ },
            onUnlockClick = { /* no-op */ },
            onAddCardClick = { /* no-op */ },
            onEditSelectedWalletClick = { /* no-op */ },
            onClearSelectedClick = { /* no-op */ },
            onDeleteSelectedWalletsClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletSelectorScreenContentPreview_Light() {
    TangemTheme {
        WalletSelectorScreenContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletSelectorScreenContentPreview_Dark() {
    TangemTheme(isDark = true) {
        WalletSelectorScreenContentSample()
    }
}

@Composable
private fun WalletSelectorScreenContent_EditWallets_Sample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        WalletSelectorScreenContent(
            state = MockData.state
                .copy(editingUserWalletsIds = listOf(MockData.state.multiCurrencyWallets[2].id)),
            onWalletClick = { /* no-op */ },
            onWalletLongClick = { /* no-op */ },
            onUnlockClick = { /* no-op */ },
            onAddCardClick = { /* no-op */ },
            onEditSelectedWalletClick = { /* no-op */ },
            onClearSelectedClick = { /* no-op */ },
            onDeleteSelectedWalletsClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletSelectorScreenContent_EditWallets_Preview_Light() {
    TangemTheme {
        WalletSelectorScreenContent_EditWallets_Sample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletSelectorScreenContent_EditWallets_Preview_Dark() {
    TangemTheme(isDark = true) {
        WalletSelectorScreenContent_EditWallets_Sample()
    }
}
// endregion Preview
