package com.tangem.managetokens.presentation.addcustomtoken.state

internal data class ButtonState(
    val isEnabled: Boolean,
    val onClick: () -> Unit,
)