package com.tangem.tap.features.details.ui.appsettings

import com.tangem.tap.features.details.redux.PrivacySetting

data class AppSettingsScreenState(
    val settings: Map<PrivacySetting, Boolean>,
    val onSettingToggled: (PrivacySetting, Boolean) -> Unit,
)


