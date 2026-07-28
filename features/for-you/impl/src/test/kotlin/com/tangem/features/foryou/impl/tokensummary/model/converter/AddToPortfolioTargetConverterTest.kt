package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.features.foryou.impl.tokensummary.model.AddToPortfolioTarget
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddToPortfolioTargetConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val converter = AddToPortfolioTargetConverter()

    @Test
    fun `GIVEN a portfolio token WHEN converted THEN it is added on the network the summary was opened for`() {
        // Arrange
        val token = currencyFactory.createToken(Blockchain.Ethereum)

        // Act
        val actual = converter.convert(TokenSummaryComponent.Token.Portfolio(cryptoCurrency = token))

        // Assert
        val expected = AddToPortfolioTarget(
            token = RawMarketToken(id = token.id.rawCurrencyId!!, name = token.name, symbol = token.symbol),
            networks = listOf(
                TokenMarketInfo.Network(
                    networkId = token.network.rawId,
                    isExchangeable = false,
                    contractAddress = token.contractAddress,
                    decimalCount = token.decimals,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a coin WHEN converted THEN it is added without a contract address`() {
        // Arrange
        val coin = currencyFactory.createCoin(Blockchain.Ethereum)

        // Act
        val actual = converter.convert(TokenSummaryComponent.Token.Portfolio(cryptoCurrency = coin))

        // Assert
        assertThat(actual?.networks?.single()?.contractAddress).isNull()
    }

    @Test
    fun `GIVEN a token with no market identity WHEN converted THEN there is nothing to add`() {
        // Arrange — a custom token has no raw id, so the flow cannot look it up
        val customToken = mockk<CryptoCurrency.Token> {
            every { id } returns mockk { every { rawCurrencyId } returns null }
        }

        // Act
        val actual = converter.convert(TokenSummaryComponent.Token.Portfolio(cryptoCurrency = customToken))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a market token WHEN converted THEN the networks resolved by the caller are used`() {
        // Arrange
        val networks = listOf(
            TokenMarketInfo.Network(networkId = "ethereum", isExchangeable = true, contractAddress = "0x", 18),
            TokenMarketInfo.Network(networkId = "polygon-pos", isExchangeable = false, contractAddress = "0x", 18),
        )

        // Act
        val actual = converter.convert(marketToken(networks))

        // Assert
        val expected = AddToPortfolioTarget(
            token = RawMarketToken(id = CryptoCurrency.RawID(value = "ethereum"), name = "Ethereum", symbol = "ETH"),
            networks = networks,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a market token without networks WHEN converted THEN there is nothing to add`() {
        // Arrange — the caller opened the summary before the token info arrived
        // Act
        val actual = converter.convert(marketToken(networks = emptyList()))

        // Assert
        assertThat(actual).isNull()
    }

    private fun marketToken(networks: List<TokenMarketInfo.Network>) = TokenSummaryComponent.Token.Market(
        cryptoCurrencyRawId = CryptoCurrency.RawID(value = "ethereum"),
        symbol = "ETH",
        title = "Ethereum",
        tangemIconUrl = "",
        networks = networks,
    )
}