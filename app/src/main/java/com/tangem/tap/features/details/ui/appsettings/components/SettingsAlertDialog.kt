package com.tangem.tap.features.details.ui.appsettings.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.details.ui.appsettings.AppSettingsDialogsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Dialog
import com.tangem.wallet.R

@Composable
internal fun SettingsAlertDialog(dialog: Dialog.Alert) {
    BasicDialog(
        title = dialog.title.resolveReference(),
        message = dialog.description.resolveReference(),
        isDismissable = false,
        confirmButton = DialogButtonUM(
            title = dialog.confirmText.resolveReference(),
            warning = true,
            onClick = dialog.onConfirm,
        ),
        dismissButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_cancel),
            onClick = dialog.onDismiss,
        ),
        onDismissDialog = dialog.onDismiss,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AlertDialogPreview(@PreviewParameter(AlertDialogProvider::class) dialog: Dialog.Alert) {
    TangemThemePreview {
        SettingsAlertDialog(dialog = dialog)
    }
}

private class AlertDialogProvider : CollectionPreviewParameterProvider<Dialog.Alert>(
    collection = buildList {
        val dialogsFactory = AppSettingsDialogsFactory()

        add(dialogsFactory.createDeleteSavedAccessCodesAlert({}, {}))
        add(dialogsFactory.createDeleteSavedWalletsAlert({}, {}))
    },
)
// endregion Preview