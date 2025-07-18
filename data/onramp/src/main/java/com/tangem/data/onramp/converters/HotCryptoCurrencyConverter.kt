package com.tangem.data.onramp.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Converter from [HotCryptoResponse.Token] to [HotCryptoCurrency]
 *
 * @property userWallet        scan response
 * @property imageHost           image host
 * @param excludedBlockchains    excluded blockchains
 *
[REDACTED_AUTHOR]
 */
internal class HotCryptoCurrencyConverter(
    private val userWallet: UserWallet,
    private val imageHost: String?,
    excludedBlockchains: ExcludedBlockchains,
) : Converter<HotCryptoResponse.Token, HotCryptoCurrency?> {

    private val cryptoCurrencyFactory by lazy(LazyThreadSafetyMode.NONE) { CryptoCurrencyFactory(excludedBlockchains) }
    private val networkFactory by lazy(LazyThreadSafetyMode.NONE) { NetworkFactory(excludedBlockchains) }

    override fun convert(value: HotCryptoResponse.Token): HotCryptoCurrency? {
        val rawId = value.id?.let(CryptoCurrency::RawID) ?: return null
        val network = value.networkId?.let(::createNetwork) ?: return null
        val currency = createCryptoCurrencyOrNull(rawId, value, network) ?: return null

        return HotCryptoCurrency(
            cryptoCurrency = currency.setupIconUrl(id = rawId.value),
            quoteStatus = createQuote(
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

        return networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
        )
    }

    private fun createQuote(
        fiatRate: BigDecimal?,
        priceChange: BigDecimal?,
        rawCurrencyId: CryptoCurrency.RawID,
    ): QuoteStatus {
        return if (fiatRate != null && priceChange != null) {
            QuoteStatus(
                rawCurrencyId = rawCurrencyId,
                value = QuoteStatus.Data(
                    fiatRate = fiatRate,
                    priceChange = priceChange.movePointLeft(2),
                    source = StatusSource.ACTUAL, // It doesn't matter
                ),
            )
        } else {
            QuoteStatus(rawCurrencyId = rawCurrencyId)
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