package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeedTokenSummaryTokenMappingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a token summary token WHEN mapped to the route and back THEN it is unchanged`(
        token: TokenSummaryComponent.Token,
    ) {
        // Act
        val actual = token.toRouteToken().toComponentToken()

        // Assert
        assertThat(actual).isEqualTo(token)
    }

    private fun provideTestModels() = listOf(
        TokenSummaryComponent.Token.Portfolio(
            cryptoCurrency = MockCryptoCurrencyFactory().createToken(blockchain = Blockchain.Ethereum),
        ),
        TokenSummaryComponent.Token.Market(
            cryptoCurrencyRawId = CryptoCurrency.RawID("bitcoin"),
            symbol = "BTC",
            title = "Bitcoin",
            tangemIconUrl = "https://example.com/bitcoin.png",
            networks = listOf(
                TokenMarketInfo.Network(
                    networkId = "bitcoin",
                    isExchangeable = true,
                    contractAddress = null,
                    decimalCount = 8,
                ),
            ),
        ),
    )
}