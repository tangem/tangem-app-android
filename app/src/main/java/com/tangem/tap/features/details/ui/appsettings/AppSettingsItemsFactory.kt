package com.tangem.tap.features.details.ui.appsettings

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Item
import com.tangem.wallet.R

internal class AppSettingsItemsFactory {

    fun createEnrollBiometricsCard(onClick: () -> Unit): Item.Card {
        return Item.Card(
            id = ID_ENROLL_BIOMETRICS_CARD,
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
            id = ID_SAVE_WALLETS_SWITCH,
            title = resourceReference(R.string.app_settings_saved_wallet),
            description = resourceReference(R.string.app_settings_saved_wallet_footer),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }

    fun createUseBiometricsSwitch(
        isChecked: Boolean,
        isEnabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ): Item.Switch {
        return Item.Switch(
            id = ID_USE_BIOMETRICS_SWITCH,
            title = resourceReference(R.string.app_settings_enable_biometrics_title),
            description = resourceReference(
                R.string.app_settings_biometrics_footer,
                wrappedList(resourceReference(R.string.common_biometrics)),
            ),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }

    fun createRequireAccessCodeSwitch(
        isChecked: Boolean,
        isEnabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ): Item.Switch {
        return Item.Switch(
            id = ID_REQUIRE_ACCESS_CODE_SWITCH,
            title = resourceReference(R.string.app_settings_require_access_code),
            description = resourceReference(R.string.app_settings_require_access_code_footer),
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
            id = ID_SAVE_ACCESS_CODES_SWITCH,
            title = resourceReference(R.string.app_settings_saved_access_codes),
            description = resourceReference(R.string.app_settings_saved_access_codes_footer),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }

    fun createFlipToHideBalanceSwitch(
        isChecked: Boolean,
        isEnabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ): Item.Switch {
        return Item.Switch(
            id = ID_FLIP_TO_HIDE_BALANCE_SWITCH,
            title = resourceReference(R.string.details_row_title_flip_to_hide),
            description = resourceReference(R.string.details_row_description_flip_to_hide),
            isEnabled = isEnabled,
            isChecked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }

    fun createSelectAppCurrencyButton(currentAppCurrencyName: String, onClick: () -> Unit): Item.Button {
        return Item.Button(
            id = ID_SELECT_APP_CURRENCY_BUTTON,
            title = resourceReference(R.string.details_row_title_currency),
            description = stringReference(currentAppCurrencyName),
            isEnabled = true,
            onClick = onClick,
        )
    }

    fun createSelectThemeModeButton(currentThemeMode: AppThemeMode, onClick: () -> Unit): Item.Button {
        return Item.Button(
            id = ID_SELECT_THEME_MODE_BUTTON,
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

    companion object {
        const val ID_ENROLL_BIOMETRICS_CARD = "enroll_biometrics_card"
        const val ID_SAVE_WALLETS_SWITCH = "save_wallets_switch"
        const val ID_SAVE_ACCESS_CODES_SWITCH = "save_access_codes_switch"
        const val ID_FLIP_TO_HIDE_BALANCE_SWITCH = "flip_to_hide_balance_switch"
        const val ID_SELECT_APP_CURRENCY_BUTTON = "select_app_currency_button"
        const val ID_SELECT_THEME_MODE_BUTTON = "select_theme_mode_button"
        const val ID_USE_BIOMETRICS_SWITCH = "use_biometrics_switch"
        const val ID_REQUIRE_ACCESS_CODE_SWITCH = "require_access_code_switch"
    }
}