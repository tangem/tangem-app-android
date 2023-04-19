package com.tangem.feature.onboarding.presentation.wallet2.model

/**
 * @author by Anton Zhilenkov on 14.03.2023.
 */
data class ButtonState(
    val enabled: Boolean = true,
    val isClickable: Boolean = true,
    val showProgress: Boolean = false,
    val onClick: () -> Unit,
)
