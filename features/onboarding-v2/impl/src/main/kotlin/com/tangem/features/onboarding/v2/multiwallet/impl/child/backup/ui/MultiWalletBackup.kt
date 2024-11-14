package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state.MultiWalletBackupUM

@Composable
fun MultiWalletBackup(state: MultiWalletBackupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        when (state) {
            is MultiWalletBackupUM.Wallet1 -> Wallet1Content(state)
            is MultiWalletBackupUM.Wallet2 -> TODO()
        }
    }

    val dialog = state.dialog
    if (dialog != null) {
        BasicDialog(
            title = dialog.title.resolveReference(),
            message = dialog.bodyText.resolveReference(),
            confirmButton = DialogButtonUM(
                title = dialog.confirmText.resolveReference(),
                onClick = dialog.onConfirm,
            ),
            dismissButton = if (dialog.cancelText != null && dialog.onCancel != null) {
                DialogButtonUM(
                    title = dialog.cancelText.resolveReference(),
                    warning = dialog.warningCancelColor,
                    onClick = dialog.onCancel,
                )
            } else {
                null
            },
            onDismissDialog = dialog.onDismiss,
        )
    }
}

@Composable
private fun ColumnScope.Wallet1Content(state: MultiWalletBackupUM.Wallet1) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 74.dp)
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // artwork

        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = state.bodyText.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    if (state.showFinalizeButton) {
        SecondaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            enabled = state.addBackupButtonEnabled,
            showProgress = state.addBackupButtonLoading,
            text = stringResource(R.string.onboarding_button_add_backup_card),
            onClick = state.onAddBackupClick,
        )

        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.onboarding_button_finalize_backup),
            onClick = state.onFinalizeButtonClick,
        )
    } else {
        PrimaryButtonIconEnd(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            iconResId = R.drawable.ic_tangem_24,
            enabled = state.addBackupButtonEnabled,
            showProgress = state.addBackupButtonLoading,
            text = stringResource(R.string.onboarding_button_add_backup_card),
            onClick = state.onAddBackupClick,
        )

        SecondaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.onboarding_button_skip_backup),
            onClick = state.onSkipButtonClick,
        )
    }
}

@Preview
@Composable
private fun PreviewWallet1() {
    TangemThemePreview {
        MultiWalletBackup(
            state = MultiWalletBackupUM.Wallet1(
                title = stringReference("Title"),
                bodyText = stringReference("Body body body"),
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewWallet2() {
    TangemThemePreview {
        MultiWalletBackup(
            state = MultiWalletBackupUM.Wallet2(
                isRing = false,
                backupAdded = true,
                onAddBackupClick = {},
                onFinalizeButtonClick = {},
            ),
        )
    }
}
