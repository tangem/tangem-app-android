package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
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
        Content(state)
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
private fun ColumnScope.Content(state: MultiWalletBackupUM) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

    SecondaryButtonIconEnd(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .fillMaxWidth(),
        iconResId = R.drawable.ic_tangem_24,
        enabled = state.addBackupButtonEnabled,
        showProgress = state.addBackupButtonLoading,
        text = stringResourceSafe(R.string.onboarding_button_add_backup_card),
        onClick = state.onAddBackupClick,
    )

    PrimaryButton(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth(),
        text = stringResourceSafe(R.string.onboarding_button_finalize_backup),
        enabled = state.finalizeButtonEnabled,
        onClick = state.onFinalizeButtonClick,
    )
}

@Preview
@Composable
private fun PreviewWallet1() {
    TangemThemePreview {
        MultiWalletBackup(
            state = MultiWalletBackupUM(
                title = stringReference("Title"),
                bodyText = stringReference("Body body body"),
            ),
        )
    }
}