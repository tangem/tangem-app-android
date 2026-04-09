package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.SelectorDialog
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.features.details.ui.appsettings.model.AppSettingsDialogConfig
import com.tangem.wallet.R
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun SettingsSelectorDialog(
    config: AppSettingsDialogConfig.ThemeModeSelector,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val modes = AppThemeMode.available
    SelectorDialog(
        title = stringResourceSafe(R.string.app_settings_theme_selector_title),
        selectedItemIndex = config.selectedModeIndex,
        items = modes.map { mode ->
            stringResourceSafe(
                id = when (mode) {
                    AppThemeMode.FORCE_DARK -> R.string.app_settings_theme_mode_dark
                    AppThemeMode.FORCE_LIGHT -> R.string.app_settings_theme_mode_light
                    AppThemeMode.FOLLOW_SYSTEM -> R.string.app_settings_theme_mode_system
                },
            )
        }.toImmutableList(),
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(R.string.common_cancel),
            onClick = onDismiss,
        ),
        onSelect = onSelect,
        onDismissDialog = onDismiss,
    )
}