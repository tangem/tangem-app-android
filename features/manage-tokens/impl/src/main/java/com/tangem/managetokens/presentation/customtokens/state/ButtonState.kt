package com.tangem.managetokens.presentation.customtokens.state

internal data class ButtonState(
    val isEnabled: Boolean,
    val onClick: () -> Unit,
)