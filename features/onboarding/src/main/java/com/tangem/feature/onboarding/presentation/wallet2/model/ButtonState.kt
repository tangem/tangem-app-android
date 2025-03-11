package com.tangem.feature.onboarding.presentation.wallet2.model

/**
[REDACTED_AUTHOR]
 */
data class ButtonState(
    val enabled: Boolean = true,
    val isClickable: Boolean = true,
    val showProgress: Boolean = false,
    val onClick: () -> Unit,
)