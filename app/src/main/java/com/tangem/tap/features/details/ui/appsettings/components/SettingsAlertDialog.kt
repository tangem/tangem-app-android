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
import com.tangem.tap.features.details.ui.appsettings.AppSettingsAlertsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Alert
import com.tangem.wallet.R

@Composable
internal fun SettingsAlertDialog(alert: Alert) {
    BasicDialog(
        title = alert.title.resolveReference(),
        message = alert.description.resolveReference(),
        isDismissable = false,
        confirmButton = DialogButton(
            title = alert.confirmText.resolveReference(),
            warning = true,
            onClick = alert.onConfirm,
        ),
        dismissButton = DialogButton(
            title = stringResource(id = R.string.common_cancel),
            onClick = alert.onDismiss,
        ),
        onDismissDialog = alert.onDismiss,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AlertDialogPreview_Light(@PreviewParameter(AlertDialogProvider::class) dialog: Alert) {
    TangemTheme {
        SettingsAlertDialog(alert = dialog)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AlertDialogPreview_Dark(@PreviewParameter(AlertDialogProvider::class) dialog: Alert) {
    TangemTheme(isDark = true) {
        SettingsAlertDialog(alert = dialog)
    }
}

private class AlertDialogProvider : CollectionPreviewParameterProvider<Alert>(
    collection = buildList {
        val alertsFactory = AppSettingsAlertsFactory()

        alertsFactory.createDeleteSavedAccessCodesAlert({}, {}).let(::add)
        alertsFactory.createDeleteSavedWalletsAlert({}, {}).let(::add)
    },
)
// endregion Preview