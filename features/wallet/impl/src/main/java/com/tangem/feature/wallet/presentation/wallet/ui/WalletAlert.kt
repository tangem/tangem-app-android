package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.AdditionalTextInputDialogUM
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState

@Composable
internal fun WalletAlert(state: WalletAlertState, onDismiss: () -> Unit) {
    when (state) {
        is WalletAlertState.Basic -> BasicAlert(state, onDismiss)
        is WalletAlertState.TextInput -> TextInputAlert(state, onDismiss)
        is WalletAlertState.SimpleOkAlert -> {
            BasicDialog(
                message = state.message.resolveReference(),
                confirmButton = DialogButtonUM(
                    onClick = {
                        state.onOkClick()
                        onDismiss()
                    },
                ),
                onDismissDialog = onDismiss,
                isDismissable = false,
            )
        }
    }
}

@Composable
private fun BasicAlert(state: WalletAlertState.Basic, onDismiss: () -> Unit) {
    val confirmButton: DialogButtonUM
    val dismissButton: DialogButtonUM?

    val onActionClick = state.onConfirmClick
    if (onActionClick != null) {
        confirmButton = DialogButtonUM(
            title = state.confirmButtonText.resolveReference(),
            warning = state.isWarningConfirmButton,
            onClick = {
                onActionClick()
                onDismiss()
            },
        )
        dismissButton = DialogButtonUM(
            title = stringResource(id = R.string.common_cancel),
            onClick = onDismiss,
        )
    } else {
        confirmButton = DialogButtonUM(
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

@Composable
private fun TextInputAlert(state: WalletAlertState.TextInput, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(text = state.text)) }

    TextInputDialog(
        fieldValue = value,
        confirmButton = DialogButtonUM(
            title = state.confirmButtonText.resolveReference(),
            enabled = value.text.isNotEmpty() &&
                value.text != state.text &&
                state.errorTextProvider(value.text) == null,
            onClick = {
                state.onConfirmClick(value.text)
                onDismiss()
            },
        ),
        onDismissDialog = onDismiss,
        onValueChange = { value = it },
        title = state.title.resolveReference(),
        dismissButton = DialogButtonUM(title = stringResource(id = R.string.common_cancel), onClick = onDismiss),
        textFieldParams = AdditionalTextInputDialogUM(
            label = state.label.resolveReference(),
            isError = state.errorTextProvider(value.text) != null,
            caption = state.errorTextProvider(value.text)?.resolveReference(),
        ),
    )
}