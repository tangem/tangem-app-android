package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState

@Composable
internal fun WalletAlert(config: WalletAlertState, onDismiss: () -> Unit) {
    when (config) {
        is WalletAlertState.WalletAlreadySignedHashes -> {
            WalletAlreadySignedHashesAlert(config = config, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun WalletAlreadySignedHashesAlert(config: WalletAlertState.WalletAlreadySignedHashes, onDismiss: () -> Unit) {
    BasicDialog(
        message = stringResource(id = R.string.alert_signed_hashes_message),
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_understand),
            onClick = {
                config.onUnderstandClick()
                onDismiss()
            },
        ),
        onDismissDialog = onDismiss,
        title = stringResource(id = R.string.warning_important_security_info, "\u26A0"),
        dismissButton = DialogButton(title = stringResource(id = R.string.common_cancel), onClick = onDismiss),
    )
}