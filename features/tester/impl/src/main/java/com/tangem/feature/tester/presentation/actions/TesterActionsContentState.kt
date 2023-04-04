package com.tangem.feature.tester.presentation.actions

internal data class TesterActionsContentState(
    val onBackClick: () -> Unit,
    val hideAllCurrencies: HideAllCurrenciesState,
)

internal sealed interface HideAllCurrenciesState {
    data class Clickable(val onClick: () -> Unit) : HideAllCurrenciesState

    object Progress : HideAllCurrenciesState
}
