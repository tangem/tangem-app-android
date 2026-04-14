package com.tangem.tap.features.details.ui.appsettings.model

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppSettingsDialogConfig {

    @Serializable
    data class ThemeModeSelector(val selectedModeIndex: Int) : AppSettingsDialogConfig
}