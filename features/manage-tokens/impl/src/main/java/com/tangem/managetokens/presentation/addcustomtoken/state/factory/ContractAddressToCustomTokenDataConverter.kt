package com.tangem.managetokens.presentation.addcustomtoken.state.factory

import com.tangem.managetokens.presentation.addcustomtoken.state.CustomTokenData
import com.tangem.managetokens.presentation.addcustomtoken.state.TextFieldState
import com.tangem.managetokens.presentation.addcustomtoken.viewmodels.AddCustomTokenClickIntents
import com.tangem.utils.converter.Converter

internal class ContractAddressToCustomTokenDataConverter(
    private val clickIntents: AddCustomTokenClickIntents,
) : Converter<String, CustomTokenData> {
    override fun convert(value: String): CustomTokenData {
        return CustomTokenData(
            contractAddressTextField = TextFieldState.Editable(
                value = value,
                isEnabled = true,
                onValueChange = clickIntents::onContractAddressChange,
                onFocusExit = clickIntents::onContractAddressFocusExit,
            ),
            nameTextField = TextFieldState.Editable(
                value = "",
                isEnabled = true,
                onValueChange = clickIntents::onTokenNameChange,
                onFocusExit = clickIntents::onTokenNameFocusExit,
            ),
            symbolTextField = TextFieldState.Editable(
                value = "",
                isEnabled = true,
                onValueChange = clickIntents::onSymbolChange,
                onFocusExit = clickIntents::onSymbolFocusExit,
            ),
            decimalsTextField = TextFieldState.Editable(
                value = "",
                isEnabled = true,
                onValueChange = clickIntents::onDecimalsChange,
                onFocusExit = clickIntents::onDecimalsFocusExit,
            ),
        )
    }
}
