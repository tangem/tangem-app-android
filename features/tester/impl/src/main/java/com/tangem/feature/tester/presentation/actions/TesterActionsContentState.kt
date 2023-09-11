package com.tangem.feature.tester.presentation.actions

import com.tangem.domain.apptheme.model.AppThemeMode

internal data class TesterActionsContentState(
    val onBackClick: () -> Unit,
    val hideAllCurrenciesConfig: HideAllCurrenciesConfig,
    val toggleAppThemeConfig: ToggleAppThemeConfig,
)

internal sealed class HideAllCurrenciesConfig {
    data class Clickable(val onClick: () -> Unit) : HideAllCurrenciesConfig()

    object Progress : HideAllCurrenciesConfig()
}

internal data class ToggleAppThemeConfig(
    val currentAppTheme: AppThemeMode,
    val onClick: () -> Unit,
)