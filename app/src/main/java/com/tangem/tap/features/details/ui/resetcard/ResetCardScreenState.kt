package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.features.details.ui.cardsettings.TextReference

data class ResetCardScreenState(
    val accepted: Boolean = false,
    val descriptionText: TextReference,
    val onAcceptWarningToggleClick: (Boolean) -> Unit,
    val onResetButtonClick: () -> Unit,
) {
    val resetButtonEnabled: Boolean
        get() = accepted
}
