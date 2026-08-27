package com.tangem.features.foryou.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.foryou.TokenSummaryComponent
import org.junit.jupiter.api.Test

/**
 * The single place where the summary token's `Token` / `Blockchain` values are resolved, so the
 * absent-network case is pinned here rather than only through the model that reports it.
 */
internal class SummaryTokenAnalyticsExtTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = currencyFactory.createCoin(Blockchain.Ethereum)

    @Test
    fun `GIVEN portfolio token WHEN resolved THEN its currency's symbol and network are returned`() {
        // Arrange
        val token = TokenSummaryComponent.Token.Portfolio(cryptoCurrency = ethereum)

        // Act
        val actual = token.toAnalyticsTokenAndNetwork()

        // Assert
        assertThat(actual).isEqualTo(ethereum.symbol to ethereum.network.name)
    }

    @Test
    fun `GIVEN portfolio token WHEN resolved THEN a token's network is returned not its contract`() {
        // Arrange
        val erc20 = currencyFactory.createToken(Blockchain.Ethereum)
        val token = TokenSummaryComponent.Token.Portfolio(cryptoCurrency = erc20)

        // Act
        val actual = token.toAnalyticsTokenAndNetwork()

        // Assert
        assertThat(actual).isEqualTo(erc20.symbol to erc20.network.name)
    }

    @Test
    fun `GIVEN market token WHEN resolved THEN there is no network to return`() {
        // Act
        val actual = marketToken().toAnalyticsTokenAndNetwork()

        // Assert
        assertThat(actual).isEqualTo(ethereum.symbol to null)
    }

    @Test
    fun `GIVEN market token with addable networks WHEN resolved THEN they are not returned as its network`() {
        // Arrange — these are networks the token could be added to, not one the summary is about, so
        // reporting any of them would misstate which network the user was looking at
        val token = marketToken(
            networks = listOf(
                TokenMarketInfo.Network(
                    networkId = "ethereum",
                    isExchangeable = false,
                    contractAddress = null,
                    decimalCount = 18,
                ),
            ),
        )

        // Act
        val actual = token.toAnalyticsTokenAndNetwork()

        // Assert
        assertThat(actual.second).isNull()
    }

    private fun marketToken(networks: List<TokenMarketInfo.Network> = emptyList()) =
        TokenSummaryComponent.Token.Market(
            cryptoCurrencyRawId = ethereum.id.rawCurrencyId!!,
            symbol = ethereum.symbol,
            title = ethereum.name,
            tangemIconUrl = "",
            networks = networks,
        )
}