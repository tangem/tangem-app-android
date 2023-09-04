package com.tangem.tap.features.details.ui.appsettings

import com.tangem.tap.features.details.redux.AppSetting

internal data class AppSettingsScreenState(
    val settings: Map<AppSetting, Boolean> = emptyMap(),
    val showEnrollBiometricsCard: Boolean = false,
    val isTogglesEnabled: Boolean = false,
    val onSettingToggled: (AppSetting, Boolean) -> Unit = { _, _ -> /* no-op */ },
    val onEnrollBiometrics: () -> Unit = { /* no-op */ },
)