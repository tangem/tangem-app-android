package com.tangem.data.onramp.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getNetwork
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.utils.converter.Converter

/**
 * Converter from [HotCryptoResponse.Token] to [HotCryptoCurrency]
 *
 * @property scanResponse        scan response
 * @property imageHost           image host
 * @property excludedBlockchains excluded blockchains
 *
[REDACTED_AUTHOR]
 */
internal class HotCryptoCurrencyConverter(
    private val scanResponse: ScanResponse,
    private val imageHost: String?,
    private val excludedBlockchains: ExcludedBlockchains,
) : Converter<HotCryptoResponse.Token, HotCryptoCurrency> {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory(excludedBlockchains) }

    override fun convert(value: HotCryptoResponse.Token): HotCryptoCurrency {
        val network = createNetwork(value.networkId)

        val contractAddress = value.contractAddress
        val decimals = value.decimalCount
        val currency = if (contractAddress != null && decimals != null) {
            cryptoCurrencyFactory.createToken(
                network = network,
                rawId = CryptoCurrency.RawID(value.id),
                name = value.name,
                symbol = value.symbol,
                decimals = decimals,
                contractAddress = contractAddress,
            )
        } else {
            cryptoCurrencyFactory.createCoin(network = network)
        }

        return HotCryptoCurrency(
            cryptoCurrency = currency.setupIconUrl(id = value.id),
            fiatRate = value.currentPrice,
            priceChange = value.priceChangePercentage,
        )
    }

    private fun createNetwork(networkId: String): Network {
        return requireNotNull(
            getNetwork(
                blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)),
                extraDerivationPath = null,
                scanResponse = scanResponse,
                excludedBlockchains = excludedBlockchains,
            ),
        )
    }

    private fun CryptoCurrency.setupIconUrl(id: String): CryptoCurrency {
        imageHost ?: return this

        val iconUrl = "${imageHost}large/$id.png"

        return when (this) {
            is CryptoCurrency.Coin -> copy(iconUrl = iconUrl)
            is CryptoCurrency.Token -> copy(iconUrl = iconUrl)
        }
    }
}