package com.tangem.managetokens.presentation.customtokens.state.factory

import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.managetokens.presentation.customtokens.state.AddCustomTokenState
import com.tangem.managetokens.presentation.customtokens.state.CustomTokenData
import com.tangem.managetokens.presentation.customtokens.state.TextFieldState
import com.tangem.utils.converter.Converter

internal class AddCustomTokenStateToCryptoCurrencyConverter(
    private val derivationStyleProvider: DerivationStyleProvider,
) : Converter<AddCustomTokenState, CryptoCurrency> {

    override fun convert(value: AddCustomTokenState): CryptoCurrency {
        val derivationPath = value.chooseDerivationState?.selectedDerivation?.path
        val token = parseTokenOrNull(value.tokenData)

        val cryptoCurrency = if (token != null) {
            CryptoCurrencyFactory().createToken(
                token = token,
                networkId = value.chooseNetworkState.selectedNetwork?.id ?: "",
                derivationStyleProvider = derivationStyleProvider,
                extraDerivationPath = derivationPath,
            )
        } else {
            CryptoCurrencyFactory().createCoin(
                networkId = value.chooseNetworkState.selectedNetwork?.id ?: "",
                derivationStyleProvider = derivationStyleProvider,
                extraDerivationPath = derivationPath,
            )
        }
        return requireNotNull(cryptoCurrency) {
            "Unless network is not Unknown blockchain, CryptoCurrency cannot be null"
        }
    }

    @Suppress("ComplexCondition")
    private fun parseTokenOrNull(tokenData: CustomTokenData?): CryptoCurrencyFactory.Token? {
        val contractAddress = (tokenData?.contractAddressTextField as? TextFieldState.Editable)?.value
        val symbol = (tokenData?.symbolTextField as? TextFieldState.Editable)?.value
        val name = (tokenData?.nameTextField as? TextFieldState.Editable)?.value
        val decimals = (tokenData?.decimalsTextField as? TextFieldState.Editable)?.value?.toIntOrNull()
        return if (
            !contractAddress.isNullOrBlank() && !symbol.isNullOrBlank() && !name.isNullOrBlank() && decimals != null
        ) {
            CryptoCurrencyFactory.Token(
                symbol = symbol,
                name = name,
                contractAddress = contractAddress,
                decimals = decimals,
                id = null,
            )
        } else {
            null
        }
    }
}