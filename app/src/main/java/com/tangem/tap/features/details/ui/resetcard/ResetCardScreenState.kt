package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.features.details.ui.cardsettings.TextReference

internal data class ResetCardScreenState(
    val accepted: Boolean = false,
    val descriptionText: TextReference,
    val acceptCondition1Checked: Boolean = false,
    val acceptCondition2Checked: Boolean = false,
    val onAcceptCondition1ToggleClick: (Boolean) -> Unit,
    val onAcceptCondition2ToggleClick: (Boolean) -> Unit,
    val onResetButtonClick: () -> Unit,
) {
    val resetButtonEnabled: Boolean
        get() = accepted
}