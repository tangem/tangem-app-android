package com.tangem.tap.features.details.ui.appsettings

import com.tangem.core.ui.models.EnrollBiometricsDialog
import com.tangem.tap.features.details.redux.PrivacySetting

data class AppSettingsScreenState(
    val settings: Map<PrivacySetting, Boolean>,
    val enrollBiometricsDialog: EnrollBiometricsDialog?,
    val onSettingToggled: (PrivacySetting, Boolean) -> Unit,
)