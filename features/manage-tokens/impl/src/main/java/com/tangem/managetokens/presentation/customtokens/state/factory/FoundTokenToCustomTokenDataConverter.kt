package com.tangem.managetokens.presentation.customtokens.state.factory

import com.tangem.domain.tokens.model.FoundToken
import com.tangem.managetokens.presentation.customtokens.state.CustomTokenData
import com.tangem.managetokens.presentation.customtokens.state.TextFieldState
import com.tangem.managetokens.presentation.customtokens.viewmodels.CustomTokensClickIntents
import com.tangem.utils.converter.Converter

internal class FoundTokenToCustomTokenDataConverter(
    private val clickIntents: CustomTokensClickIntents,
) : Converter<FoundToken, CustomTokenData> {
    override fun convert(value: FoundToken): CustomTokenData {
        return CustomTokenData(
            contractAddressTextField = TextFieldState.Editable(
                value = value.contractAddress,
                isEnabled = true,
                onValueChange = clickIntents::onContractAddressChange,
                onFocusExit = clickIntents::onContractAddressFocusExit,
            ),
            nameTextField = TextFieldState.Editable(
                value = value.name,
                isEnabled = false,
                onValueChange = clickIntents::onTokenNameChange,
                onFocusExit = clickIntents::onTokenNameFocusExit,
            ),
            symbolTextField = TextFieldState.Editable(
                value = value.symbol,
                isEnabled = false,
                onValueChange = clickIntents::onSymbolChange,
                onFocusExit = clickIntents::onSymbolFocusExit,
            ),
            decimalsTextField = TextFieldState.Editable(
                value = value.decimals.toString(),
                isEnabled = false,
                onValueChange = clickIntents::onDecimalsChange,
                onFocusExit = clickIntents::onDecimalsFocusExit,
            ),
        )
    }
}