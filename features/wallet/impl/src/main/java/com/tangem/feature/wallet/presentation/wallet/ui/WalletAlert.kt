package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState

@Composable
internal fun WalletAlert(config: WalletAlertState, onDismiss: () -> Unit) {
    when (config) {
        is WalletAlertState.WalletAlreadySignedHashes -> {
            WalletAlreadySignedHashesAlert(config = config, onDismiss = onDismiss)
        }
        is WalletAlertState.DefaultAlert -> {
            DefaultAlert(config = config, onDismiss = onDismiss)
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

@Composable
private fun DefaultAlert(config: WalletAlertState.DefaultAlert, onDismiss: () -> Unit) {
    val confirmButton: DialogButton
    val dismissButton: DialogButton?
    if (config.onActionClick != null) {
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            onClick = {
                config.onActionClick.invoke()
                onDismiss()
            },
        )
        dismissButton = DialogButton(
            title = stringResource(id = R.string.common_cancel),
            onClick = onDismiss,
        )
    } else {
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            onClick = onDismiss,
        )
        dismissButton = null
    }
    BasicDialog(
        message = config.message.resolveReference(),
        confirmButton = confirmButton,
        onDismissDialog = onDismiss,
        title = config.title.resolveReference(),
        dismissButton = dismissButton,
    )
}