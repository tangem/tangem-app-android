package com.tangem.tap

import com.tangem.domain.apptheme.model.AppThemeMode

internal sealed class GlobalSettingsState {

    object Loading : GlobalSettingsState()

    data class Content(
        val appThemeMode: AppThemeMode,
    ) : GlobalSettingsState()
}