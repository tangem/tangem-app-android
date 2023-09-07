package com.tangem.tap.features.details.ui.appsettings

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Item
import com.tangem.wallet.R

internal class AppSettingsItemsFactory {

    fun createEnrollBiometricsCard(onClick: () -> Unit): Item.Card {
        return Item.Card(
            id = "enroll_biometrics_card",
            title = resourceReference(R.string.app_settings_enable_biometrics_title),
            description = resourceReference(R.string.app_settings_enable_biometrics_description),
            iconResId = R.drawable.ic_alert_circle_24,
            onClick = onClick,
        )
    }

    fun createSaveWalletsSwitch(
        isChecked: Boolean,
        isEnabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ): Item.Switch {
        return Item.Switch(
            id = "save_wallets_switch",
            title = resourceReference(R.string.app_settings_saved_wallet),
            description = resourceReference(R.string.app_settings_saved_wallet_footer),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }

    fun createSaveAccessCodeSwitch(
        isChecked: Boolean,
        isEnabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ): Item.Switch {
        return Item.Switch(
            id = "save_access_codes_switch",
            title = resourceReference(R.string.app_settings_saved_access_codes),
            description = resourceReference(R.string.app_settings_saved_access_codes_footer),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }

    fun createSelectAppCurrencyButton(currentAppCurrencyName: String, onClick: () -> Unit): Item.Button {
        return Item.Button(
            id = "select_app_currency_button",
            title = resourceReference(R.string.details_row_title_currency),
            description = stringReference(currentAppCurrencyName),
            isEnabled = true,
            onClick = onClick,
        )
    }

    fun createSelectThemeModeButton(currentThemeMode: AppThemeMode, onClick: () -> Unit): Item.Button {
        return Item.Button(
            id = "select_theme_mode_button",
            title = resourceReference(R.string.app_settings_theme_selector_title),
            description = resourceReference(
                id = when (currentThemeMode) {
                    AppThemeMode.FORCE_DARK -> R.string.app_settings_theme_mode_dark
                    AppThemeMode.FORCE_LIGHT -> R.string.app_settings_theme_mode_light
                    AppThemeMode.FOLLOW_SYSTEM -> R.string.app_settings_theme_mode_system
                },
            ),
            isEnabled = true,
            onClick = onClick,
        )
    }
}