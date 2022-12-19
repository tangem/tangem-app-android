package com.tangem.tap.features.details.ui.resetcard

import androidx.annotation.StringRes

data class ResetCardScreenState(
    @StringRes val descriptionResId: Int,
    val accepted: Boolean = false,
    val onAcceptWarningToggleClick: (Boolean) -> Unit,
    val onResetButtonClick: () -> Unit,
) {
    val resetButtonEnabled: Boolean
        get() = accepted
}
