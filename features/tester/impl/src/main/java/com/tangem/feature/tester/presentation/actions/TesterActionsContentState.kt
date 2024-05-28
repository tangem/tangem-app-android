package com.tangem.feature.tester.presentation.actions

import com.tangem.domain.apptheme.model.AppThemeMode

internal data class TesterActionsContentState(
    val hideAllCurrenciesConfig: HideAllCurrenciesConfig,
    val toggleAppThemeConfig: ToggleAppThemeConfig,
    val onBackClick: () -> Unit,
    val onApplyChangesClick: () -> Unit,
)

internal sealed class HideAllCurrenciesConfig {
    data class Clickable(val onClick: () -> Unit) : HideAllCurrenciesConfig()

    data object Progress : HideAllCurrenciesConfig()
}

internal data class ToggleAppThemeConfig(
    val currentAppTheme: AppThemeMode,
    val onClick: () -> Unit,
)
