package com.tangem.tap.features.details.ui.appsettings

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Dialog
import com.tangem.wallet.R
import kotlinx.collections.immutable.toImmutableList

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

    fun createThemeModeSelectorDialog(
        selectedModeIndex: Int,
        onSelect: (AppThemeMode) -> Unit,
        onDismiss: () -> Unit,
    ): Dialog.Selector {
        val modes = AppThemeMode.available

        return Dialog.Selector(
            title = resourceReference(R.string.app_settings_theme_selector_title),
            selectedItemIndex = selectedModeIndex,
            items = modes.map { mode ->
                resourceReference(
                    id = when (mode) {
                        AppThemeMode.FORCE_DARK -> R.string.app_settings_theme_mode_dark
                        AppThemeMode.FORCE_LIGHT -> R.string.app_settings_theme_mode_light
                        AppThemeMode.FOLLOW_SYSTEM -> R.string.app_settings_theme_mode_system
                    },
                )
            }.toImmutableList(),
            onSelect = { index ->
                val mode = AppThemeMode.available[index]

                onSelect(mode)
            },
            onDismiss = onDismiss,
        )
    }
}