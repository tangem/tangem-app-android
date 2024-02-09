package com.tangem.managetokens.presentation.addcustomtoken.state

internal data class EnterCustomDerivationState(
    val value: String,
    val onValueChange: (String) -> Unit,
    val confirmButtonEnabled: Boolean,
    val derivationIncorrect: Boolean,
    val onConfirmButtonClick: () -> Unit,
    val onDismiss: () -> Unit,
)
