package com.tangem.managetokens.presentation.customtokens.state

internal data class CustomTokenData(
    val contractAddressTextField: TextFieldState,
    val nameTextField: TextFieldState,
    val symbolTextField: TextFieldState,
    val decimalsTextField: TextFieldState,
) {

    fun isRequiredInformationProvided(): Boolean {
        return contractAddressTextField.isInputValid() && nameTextField.isInputValid() &&
            symbolTextField.isInputValid() && decimalsTextField.isInputValid()
    }
}