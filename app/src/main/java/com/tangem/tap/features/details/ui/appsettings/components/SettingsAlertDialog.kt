package com.tangem.tap.features.details.ui.appsettings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.appsettings.AppSettingsDialogsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Dialog
import com.tangem.wallet.R

@Composable
internal fun SettingsAlertDialog(dialog: Dialog.Alert) {
    BasicDialog(
        title = dialog.title.resolveReference(),
        message = dialog.description.resolveReference(),
        isDismissable = false,
        confirmButton = DialogButton(
            title = dialog.confirmText.resolveReference(),
            warning = true,
            onClick = dialog.onConfirm,
        ),
        dismissButton = DialogButton(
            title = stringResource(id = R.string.common_cancel),
            onClick = dialog.onDismiss,
        ),
        onDismissDialog = dialog.onDismiss,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AlertDialogPreview_Light(@PreviewParameter(AlertDialogProvider::class) dialog: Dialog.Alert) {
    TangemTheme {
        SettingsAlertDialog(dialog = dialog)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AlertDialogPreview_Dark(@PreviewParameter(AlertDialogProvider::class) dialog: Dialog.Alert) {
    TangemTheme(isDark = true) {
        SettingsAlertDialog(dialog = dialog)
    }
}

private class AlertDialogProvider : CollectionPreviewParameterProvider<Dialog.Alert>(
    collection = buildList {
        val dialogsFactory = AppSettingsDialogsFactory()

        dialogsFactory.createDeleteSavedAccessCodesAlert({}, {}).let(::add)
        dialogsFactory.createDeleteSavedWalletsAlert({}, {}).let(::add)
    },
)
// endregion Preview
