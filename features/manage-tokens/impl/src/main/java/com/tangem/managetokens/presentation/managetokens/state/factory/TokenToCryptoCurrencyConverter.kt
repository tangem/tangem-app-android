package com.tangem.managetokens.presentation.managetokens.state.factory

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.utils.converter.Converter

internal class TokenToCryptoCurrencyConverter(
    private val network: NetworkItemState,
    private val derivationStyleProvider: DerivationStyleProvider,
) : Converter<TokenItemState.Loaded, CryptoCurrency?> {

    override fun convert(value: TokenItemState.Loaded): CryptoCurrency? {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(network.id))
        val sdkToken = if (network is NetworkItemState.Toggleable && network.address != null) {
            com.tangem.blockchain.common.Token(
                symbol = value.currencySymbol,
                name = value.name,
                contractAddress = network.address,
                decimals = network.decimals!!,
                id = value.tokenId,
            )
        } else {
            null
        }
        return if (sdkToken != null) {
            CryptoCurrencyFactory().createToken(
                sdkToken = sdkToken,
                blockchain = blockchain,
                derivationStyleProvider = derivationStyleProvider,
                extraDerivationPath = null,
            )
        } else {
            CryptoCurrencyFactory().createCoin(
                blockchain = blockchain,
                derivationStyleProvider = derivationStyleProvider,
                extraDerivationPath = null,
            )
        }
    }
}