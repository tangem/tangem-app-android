package com.tangem.tap.features.details.ui.resetcard

data class ResetCardScreenState(
    val accepted: Boolean = false,
    val onAcceptWarningToggleClick: (Boolean) -> Unit,
    val onResetButtonClick: () -> Unit,
) {
    val resetButtonEnabled: Boolean
        get() = accepted
}