package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.AdditionalTextInputDialogParams
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.walletSelector.ui.model.DialogModel
import com.tangem.wallet.R

@Composable
internal fun RenameWalletDialogContent(dialog: DialogModel.RenameWalletDialog) {
    var value by remember {
        mutableStateOf(TextFieldValue(text = dialog.currentName))
    }

    TextInputDialog(
        fieldValue = value,
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_save),
            enabled = value.text.isNotEmpty() && value.text != dialog.currentName,
            onClick = { dialog.onConfirm(value.text) },
        ),
        onDismissDialog = dialog.onDismiss,
        onValueChange = { newValue ->
            value = newValue
        },
        title = stringResource(R.string.user_wallet_list_rename_popup_title),
        dismissButton = DialogButton(
            title = stringResource(id = R.string.common_cancel),
            onClick = dialog.onDismiss,
        ),
        textFieldParams = AdditionalTextInputDialogParams(
            label = stringResource(R.string.user_wallet_list_rename_popup_placeholder),
        ),
    )
}

// region Preview
@Composable
private fun RenameWalletDialogContentSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        RenameWalletDialogContent(dialog = DialogModel.RenameWalletDialog("", {}, {}))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RenameWalletDialogContentPreview_Light() {
    TangemTheme {
        RenameWalletDialogContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RenameWalletDialogContentPreview_Dark() {
    TangemTheme(isDark = true) {
        RenameWalletDialogContentSample()
    }
}
// endregion Preview
