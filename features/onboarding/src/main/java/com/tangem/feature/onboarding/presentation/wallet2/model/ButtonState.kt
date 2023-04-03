package com.tangem.feature.onboarding.presentation.wallet2.model

/**
[REDACTED_AUTHOR]
 */
data class ButtonState(
    val enabled: Boolean = true,
    val showProgress: Boolean = false,
    val onClick: () -> Unit = {},
) {
    val isClickable: Boolean = !showProgress
}