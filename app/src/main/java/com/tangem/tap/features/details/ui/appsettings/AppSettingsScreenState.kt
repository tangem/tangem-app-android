package com.tangem.tap.features.details.ui.appsettings

import com.tangem.tap.features.details.redux.PrivacySetting

data class AppSettingsScreenState(
    val settings: Map<PrivacySetting, Boolean> = emptyMap(),
    val showEnrollBiometricsCard: Boolean = false,
    val isTogglesEnabled: Boolean = true,
    val onSettingToggled: (PrivacySetting, Boolean) -> Unit = { _, _ -> /* no-op */ },
    val onEnrollBiometrics: () -> Unit = { /* no-op */ },
)
