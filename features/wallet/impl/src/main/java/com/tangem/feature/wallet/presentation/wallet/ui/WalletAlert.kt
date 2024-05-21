package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.AdditionalTextInputDialogParams
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState

@Composable
internal fun WalletAlert(state: WalletAlertState, onDismiss: () -> Unit) {
    when (state) {
        is WalletAlertState.Basic -> BasicAlert(state, onDismiss)
        is WalletAlertState.TextInput -> TextInputAlert(state, onDismiss)
    }
}

@Composable
private fun BasicAlert(state: WalletAlertState.Basic, onDismiss: () -> Unit) {
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

@Composable
private fun TextInputAlert(state: WalletAlertState.TextInput, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(text = state.text)) }

    TextInputDialog(
        fieldValue = value,
        confirmButton = DialogButton(
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
        dismissButton = DialogButton(title = stringResource(id = R.string.common_cancel), onClick = onDismiss),
        textFieldParams = AdditionalTextInputDialogParams(
            label = state.label.resolveReference(),
            isError = state.errorTextProvider(value.text) != null,
            caption = state.errorTextProvider(value.text)?.resolveReference(),
        ),
    )
}