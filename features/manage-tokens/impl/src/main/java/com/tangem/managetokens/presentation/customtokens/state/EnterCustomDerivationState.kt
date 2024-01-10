package com.tangem.managetokens.presentation.customtokens.state

internal data class EnterCustomDerivationState(
    val value: String,
    val onValueChange: (String) -> Unit,
    val confirmButtonEnabled: Boolean,
    val derivationIncorrect: Boolean,
    val onConfirmButtonClick: () -> Unit,
    val onDismiss: () -> Unit,
)