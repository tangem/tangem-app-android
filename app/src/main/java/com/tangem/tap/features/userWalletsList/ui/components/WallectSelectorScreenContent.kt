package com.tangem.tap.features.userWalletsList.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.common.ui.components.Hand
import com.tangem.tap.common.ui.components.PrimaryButton
import com.tangem.tap.common.ui.components.SecondaryButtonIconRight
import com.tangem.tap.features.userWalletsList.ui.Mock
import com.tangem.tap.features.userWalletsList.ui.WalletSelectorScreenState
import com.tangem.wallet.R

@Composable
internal fun WalletSelectorScreenContent(
    modifier: Modifier = Modifier,
    state: WalletSelectorScreenState,
    onWalletClick: (walletId: String) -> Unit,
    onWalletLongClick: (walletId: String) -> Unit,
    onUnlockClick: () -> Unit,
    onScanCardClick: () -> Unit,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    val selectedWalletsSize = remember(state.wallets) { state.selectedWalletIds.size }

    Column(
        modifier = modifier
            .background(color = colorResource(id = R.color.background_plain)),
    ) {
        Hand()
        Spacer(modifier = Modifier.height(10.dp))
        if (selectedWalletsSize > 0) {
            EditWalletsBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                selectedWalletsSize = selectedWalletsSize,
                onClearSelectedClick = onClearSelectedClick,
                onEditSelectedWalletClick = onEditSelectedWalletClick,
                onDeleteSelectedWalletsClick = onDeleteSelectedWalletsClick,
            )
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "My wallets",
                style = TangemTypography.subtitle1,
                color = colorResource(id = R.color.text_primary_1),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        WalletList(
            wallets = state.wallets,
            fiatCurrency = state.fiatCurrency,
            selectedWalletId = state.currentWalletId,
            checkedWalletIds = state.selectedWalletIds,
            isLocked = state.isLocked,
            onWalletClick = onWalletClick,
            onWalletLongClick = onWalletLongClick,
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (state.isLocked) {
            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = "Unlock all with biometrics",
                onClick = onUnlockClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        SecondaryButtonIconRight(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            text = "Add new card",
            icon = painterResource(id = R.drawable.ic_tangem),
            onClick = onScanCardClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditWalletsBar(
    modifier: Modifier = Modifier,
    selectedWalletsSize: Int,
    onClearSelectedClick: () -> Unit,
    onEditSelectedWalletClick: () -> Unit,
    onDeleteSelectedWalletsClick: () -> Unit,
) {
    val showEditAction = remember(selectedWalletsSize) { selectedWalletsSize in 1 until 2 }
    Row(
        modifier = modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier.size(32.dp),
            onClick = onClearSelectedClick,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_close),
                tint = colorResource(id = R.color.icon_primary_1),
                contentDescription = "Unselect wallets",
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "$selectedWalletsSize selected",
            style = TangemTypography.subtitle1,
            color = colorResource(id = R.color.text_primary_1),
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showEditAction) {
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onEditSelectedWalletClick,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_pencil_outline_24),
                    tint = colorResource(id = R.color.icon_primary_1),
                    contentDescription = "Edit wallet name",
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        IconButton(
            modifier = Modifier.size(32.dp),
            onClick = onDeleteSelectedWalletsClick,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_trash),
                tint = colorResource(id = R.color.icon_warning),
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
    Column(modifier = modifier.background(color = colorResource(id = R.color.background_action))) {
        WalletSelectorScreenContent(
            state = Mock.state.copy(isLocked = true),
            onWalletClick = { /* no-op */ },
            onWalletLongClick = { /* no-op */ },
            onUnlockClick = { /* no-op */ },
            onScanCardClick = { /* no-op */ },
            onEditSelectedWalletClick = { /* no-op */ },
            onClearSelectedClick = { /* no-op */ },
            onDeleteSelectedWalletsClick = { /* no-op */ },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletSelectorScreenContentPreview() {
    AppCompatTheme {
        WalletSelectorScreenContentSample()
    }
}
// endregion Preview
