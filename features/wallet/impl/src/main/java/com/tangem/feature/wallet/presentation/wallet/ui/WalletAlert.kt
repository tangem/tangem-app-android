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
        is WalletAlertState.DefaultAlert -> {
            DefaultAlert(config = config, onDismiss = onDismiss)
        }
    }
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