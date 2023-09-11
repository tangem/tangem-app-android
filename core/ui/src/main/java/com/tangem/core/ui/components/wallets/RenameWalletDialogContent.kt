package com.tangem.core.ui.components.wallets

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.AdditionalTextInputDialogParams
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.res.TangemTheme

/**
 * Rename a wallet dialog
 *
 * @param name      wallet name
 * @param onConfirm lambda be invoked when Confirm button is clicked
 * @param onDismiss lambda be invoked when dialog is dismissed
 */
@Composable
fun RenameWalletDialogContent(name: String, onConfirm: (newName: String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(text = name)) }

    TextInputDialog(
        fieldValue = value,
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            enabled = value.text.isNotEmpty() && value.text != name,
            onClick = { onConfirm(value.text) },
        ),
        onDismissDialog = onDismiss,
        onValueChange = { value = it },
        title = stringResource(R.string.user_wallet_list_rename_popup_title),
        dismissButton = DialogButton(title = stringResource(id = R.string.common_cancel), onClick = onDismiss),
        textFieldParams = AdditionalTextInputDialogParams(
            label = stringResource(R.string.user_wallet_list_rename_popup_placeholder),
        ),
    )
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RenameWalletDialogContentPreview_Light() {
    TangemTheme(isDark = false) {
        RenameWalletDialogContent(name = "", onConfirm = {}, onDismiss = {})
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RenameWalletDialogContentPreview_Dark() {
    TangemTheme(isDark = true) {
        RenameWalletDialogContent(name = "", onConfirm = {}, onDismiss = {})
    }
}

// endregion Preview