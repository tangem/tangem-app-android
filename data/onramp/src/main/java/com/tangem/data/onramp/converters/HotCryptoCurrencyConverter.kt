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
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

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
) : Converter<HotCryptoResponse.Token, HotCryptoCurrency?> {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory(excludedBlockchains) }

    override fun convert(value: HotCryptoResponse.Token): HotCryptoCurrency? {
        val rawId = value.id?.let(CryptoCurrency::RawID) ?: return null
        val network = value.networkId?.let(::createNetwork) ?: return null
        val currency = createCryptoCurrencyOrNull(rawId, value, network) ?: return null

        return HotCryptoCurrency(
            cryptoCurrency = currency.setupIconUrl(id = rawId.value),
            quote = createQuote(
                fiatRate = value.currentPrice,
                priceChange = value.priceChangePercentage,
                rawCurrencyId = rawId,
            ),
        )
    }

    private fun createCryptoCurrencyOrNull(
        rawId: CryptoCurrency.RawID,
        value: HotCryptoResponse.Token,
        network: Network,
    ): CryptoCurrency? {
        val name = value.name
        val symbol = value.symbol

        if (name.isNullOrBlank() || symbol.isNullOrBlank()) return null

        val contractAddress = value.contractAddress
        val decimals = value.decimalCount

        return if (contractAddress != null && decimals != null) {
            cryptoCurrencyFactory.createToken(
                network = network,
                rawId = rawId,
                name = name,
                symbol = symbol,
                decimals = decimals,
                contractAddress = contractAddress,
            )
        } else {
            cryptoCurrencyFactory.createCoin(network = network)
        }
    }

    private fun createNetwork(networkId: String): Network? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null

        return getNetwork(
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = scanResponse,
            excludedBlockchains = excludedBlockchains,
        )
    }

    private fun createQuote(
        fiatRate: BigDecimal?,
        priceChange: BigDecimal?,
        rawCurrencyId: CryptoCurrency.RawID,
    ): Quote {
        return if (fiatRate != null && priceChange != null) {
            Quote.Value(
                rawCurrencyId = rawCurrencyId,
                fiatRate = fiatRate,
                priceChange = priceChange.movePointLeft(2),
                isCached = false, // It doesn't matter
            )
        } else {
            Quote.Empty(rawCurrencyId)
        }
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