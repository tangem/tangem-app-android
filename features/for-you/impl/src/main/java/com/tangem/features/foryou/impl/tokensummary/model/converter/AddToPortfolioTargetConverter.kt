package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.model.AddToPortfolioTarget
import com.tangem.utils.converter.Converter

/**
 * Resolves the summary token into something the add-to-portfolio flow can add.
 *
 * A Portfolio-origin token is added on the very network the summary was opened for, a Market-origin one on the networks
 * its caller already resolved. Returns `null` when neither yields anything to add — a token with no market identity, or
 * a market token whose networks have not arrived — and the summary then makes no such offer.
 */
internal class AddToPortfolioTargetConverter :
    Converter<TokenSummaryComponent.Token, AddToPortfolioTarget?> {

    override fun convert(value: TokenSummaryComponent.Token): AddToPortfolioTarget? = when (value) {
        is TokenSummaryComponent.Token.Portfolio -> value.cryptoCurrency.toTarget()
        is TokenSummaryComponent.Token.Market -> AddToPortfolioTarget(
            token = RawMarketToken(id = value.cryptoCurrencyRawId, name = value.title, symbol = value.symbol),
            networks = value.networks,
        ).takeIf { value.networks.isNotEmpty() }
    }

    private fun CryptoCurrency.toTarget(): AddToPortfolioTarget? {
        val rawId = id.rawCurrencyId ?: return null

        return AddToPortfolioTarget(
            token = RawMarketToken(id = rawId, name = name, symbol = symbol),
            networks = listOf(
                TokenMarketInfo.Network(
                    networkId = network.rawId,
                    isExchangeable = false,
                    contractAddress = (this as? CryptoCurrency.Token)?.contractAddress,
                    decimalCount = decimals,
                ),
            ),
        )
    }
}