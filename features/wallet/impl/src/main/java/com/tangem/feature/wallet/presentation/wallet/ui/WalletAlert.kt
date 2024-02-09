package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState

@Composable
internal fun WalletAlert(state: WalletAlertState, onDismiss: () -> Unit) {
    DefaultAlert(state = state, onDismiss = onDismiss)
}

@Composable
private fun DefaultAlert(state: WalletAlertState, onDismiss: () -> Unit) {
    val confirmButton: DialogButton
    val dismissButton: DialogButton?

    val onActionClick = state.onConfirmClick
    if (onActionClick != null) {
        confirmButton = DialogButton(
            title = state.confirmButtonText.resolveReference(),
            warning = state.isWarningConfirmButton,
            onClick = {
                onActionClick()
                onDismiss()
            },
        )
        dismissButton = DialogButton(
            title = stringResource(id = R.string.common_cancel),
            onClick = onDismiss,
        )
    } else {
        confirmButton = DialogButton(
            title = state.confirmButtonText.resolveReference(),
            warning = state.isWarningConfirmButton,
            onClick = onDismiss,
        )
        dismissButton = null
    }

    BasicDialog(
        message = state.message.resolveReference(),
        confirmButton = confirmButton,
        onDismissDialog = onDismiss,
        title = state.title?.resolveReference(),
        dismissButton = dismissButton,
    )
}