package com.tangem.managetokens.presentation.customtokens.state.factory

import com.tangem.managetokens.presentation.customtokens.state.CustomTokenData
import com.tangem.managetokens.presentation.customtokens.state.TextFieldState
import com.tangem.managetokens.presentation.customtokens.viewmodels.CustomTokensClickIntents
import com.tangem.utils.converter.Converter

internal class ContractAddressToCustomTokenDataConverter(
    private val clickIntents: CustomTokensClickIntents,
) : Converter<String, CustomTokenData> {
    override fun convert(value: String): CustomTokenData {
        return CustomTokenData(
            contractAddressTextField = TextFieldState.Editable(
                value = value,
                isEnabled = true,
                onValueChange = clickIntents::onContractAddressChange,
            ),
            nameTextField = TextFieldState.Editable(
                value = "",
                isEnabled = true,
                onValueChange = clickIntents::onTokenNameChange,
            ),
            symbolTextField = TextFieldState.Editable(
                value = "",
                isEnabled = true,
                onValueChange = clickIntents::onSymbolChange,
            ),
            decimalsTextField = TextFieldState.Editable(
                value = "",
                isEnabled = true,
                onValueChange = clickIntents::onDecimalsChange,
            ),
        )
    }
}