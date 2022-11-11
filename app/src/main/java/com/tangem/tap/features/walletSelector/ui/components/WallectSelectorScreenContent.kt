package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButtonIconRight
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerW16
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.walletSelector.ui.WalletSelectorScreenState
import com.tangem.wallet.R

@Composable
internal fun WalletSelectorScreenContent(
    modifier: Modifier = Modifier,
    state: WalletSelectorScreenState,
    onWalletClick: (walletId: String) -> Unit,
    onWalletLongClick: (walletId: String) -> Unit,
    onUnlockClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    val editingWalletsSize by rememberUpdatedState(newValue = state.editingWalletsIds.size)

    Column(modifier = modifier) {
        Hand()
        SpacerH12()
        if (editingWalletsSize > 0) {
            EditWalletsBar(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                editingWalletsSize = editingWalletsSize,
                onClearSelectedClick = onClearSelectedClick,
                onEditSelectedWalletClick = onEditSelectedWalletClick,
                onDeleteSelectedWalletsClick = onDeleteSelectedWalletsClick,
            )
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.user_wallet_list_title),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
        }
        SpacerH12()
        WalletList(
            wallets = state.wallets,
            selectedWalletId = state.selectedWalletId,
            checkedWalletIds = state.editingWalletsIds,
            isLocked = state.isLocked,
            onWalletClick = onWalletClick,
            onWalletLongClick = onWalletLongClick,
        )
        SpacerH24()
        if (state.isLocked) {
            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                text = stringResource(R.string.user_wallet_list_unlock_all_face_id),
                showProgress = state.showUnlockProgress,
                onClick = onUnlockClick,
            )
            SpacerH12()
        }
        SecondaryButtonIconRight(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
            text = stringResource(R.string.user_wallet_list_add_button),
            showProgress = state.showAddCardProgress,
            icon = painterResource(id = R.drawable.ic_tangem),
            onClick = onAddCardClick,
        )
        SpacerH16()
    }
}

@Composable
private fun EditWalletsBar(
    modifier: Modifier = Modifier,
    editingWalletsSize: Int,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    val showEditAction = remember(editingWalletsSize) { editingWalletsSize in 1 until 2 }
    Row(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens.spacing8)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(TangemTheme.dimens.size32),
            onClick = onClearSelectedClick,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_close),
                tint = TangemTheme.colors.icon.primary1,
                contentDescription = "Unselect wallets",
            )
        }
        SpacerW16()
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
                    contentDescription = "Edit wallet name",
                )
            }
            SpacerW8()
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
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.plain,
                    shape = TangemTheme.shapes.bottomSheet,
                ),
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
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.plain,
                    shape = TangemTheme.shapes.bottomSheet,
                ),
            state = MockData.state
                .copy(editingWalletsIds = listOf(MockData.userWalletList[2].id)),
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
