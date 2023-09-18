package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.features.details.ui.cardsettings.TextReference

internal data class ResetCardScreenState(
    val accepted: Boolean = false,
    val descriptionText: TextReference,
    val acceptWarning1Checked: Boolean = false,
    val acceptWarning2Checked: Boolean = false,
    val onAcceptWarning1ToggleClick: (Boolean) -> Unit,
    val onAcceptWarning2ToggleClick: (Boolean) -> Unit,
    val onResetButtonClick: () -> Unit,
) {
    val resetButtonEnabled: Boolean
        get() = accepted
}
