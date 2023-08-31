package com.tangem.tap.features.details.ui.appsettings

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Dialog
import com.tangem.wallet.R

internal class AppSettingsDialogsFactory {

    fun createDeleteSavedWalletsAlert(onDelete: () -> Unit, onDismiss: () -> Unit): Dialog.Alert {
        return Dialog.Alert(
            title = resourceReference(R.string.common_attention),
            description = resourceReference(R.string.app_settings_off_saved_wallet_alert_message),
            confirmText = resourceReference(R.string.common_delete),
            onConfirm = onDelete,
            onDismiss = onDismiss,
        )
    }

    fun createDeleteSavedAccessCodesAlert(onDelete: () -> Unit, onDismiss: () -> Unit): Dialog.Alert {
        return Dialog.Alert(
            title = resourceReference(R.string.common_attention),
            description = resourceReference(R.string.app_settings_off_saved_access_code_alert_message),
            confirmText = resourceReference(R.string.common_delete),
            onConfirm = onDelete,
            onDismiss = onDismiss,
        )
    }
}
