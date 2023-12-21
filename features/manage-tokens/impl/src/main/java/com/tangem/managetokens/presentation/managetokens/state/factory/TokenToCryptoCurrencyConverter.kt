package com.tangem.managetokens.presentation.managetokens.state.factory

import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.utils.converter.Converter

internal class TokenToCryptoCurrencyConverter(
    private val network: NetworkItemState,
    private val derivationStyleProvider: DerivationStyleProvider,
) : Converter<TokenItemState.Loaded, CryptoCurrency?> {

    override fun convert(value: TokenItemState.Loaded): CryptoCurrency? {
        return if (network is NetworkItemState.Toggleable && network.address != null) {
            CryptoCurrencyFactory().createToken(
                CryptoCurrencyFactory.Token(
                    symbol = value.currencySymbol,
                    name = value.name,
                    id = value.tokenId,
                    contractAddress = network.address,
                    decimals = requireNotNull(network.decimals),
                ),
                networkId = network.id,
                derivationStyleProvider = derivationStyleProvider,
                extraDerivationPath = null,
            )
        } else {
            CryptoCurrencyFactory().createCoin(
                networkId = network.id,
                derivationStyleProvider = derivationStyleProvider,
                extraDerivationPath = null,
            )
        }
    }
}