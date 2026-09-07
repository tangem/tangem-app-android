package com.tangem.features.feed.earn.model.search

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnSearchMatchingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun matching(model: MatchingModel) {
        // Act
        val actual = model.catalog.matching(model.query)

        // Assert
        assertThat(actual.map { it.earnToken.tokenSymbol }).containsExactlyElementsIn(model.expectedSymbols).inOrder()
    }

    @Test
    fun `GIVEN a token offered as both staking and yield WHEN it matches THEN both opportunities are returned`() {
        // Arrange
        val staking = earnToken(symbol = "USDC", name = "USD Coin", type = EarnType.STAKING)
        val yieldMode = earnToken(symbol = "USDC", name = "USD Coin", type = EarnType.YIELD)

        // Act
        val actual = listOf(staking, yieldMode).matching("usdc")

        // Assert
        assertThat(actual.map { it.earnToken.type }).containsExactly(EarnType.STAKING, EarnType.YIELD)
    }

    @Test
    fun `GIVEN one token on three networks WHEN it matches THEN all three are returned in yield order`() {
        // Arrange — the real catalogue lists usd-coin on base (3.84%), ethereum (3.60%) and avalanche (3.32%)
        val catalog = listOf(
            usdc(networkId = "ethereum", apy = "0.036000"),
            usdc(networkId = "avalanche", apy = "0.033200"),
            usdc(networkId = "base", apy = "0.038400"),
        )

        // Act
        val actual = catalog.matching("usdc")

        // Assert
        assertThat(actual.map { it.earnToken.networkId })
            .containsExactly("base", "ethereum", "avalanche")
            .inOrder()
    }

    private fun usdc(networkId: String, apy: String): EarnTokenWithCurrency = createEarnTokenWithCurrency(
        earnToken = createEarnToken(
            apy = apy,
            networkId = networkId,
            tokenId = "usd-coin",
            tokenSymbol = "USDC",
            tokenName = "USDC",
            type = EarnType.YIELD,
        ),
    )

    internal data class MatchingModel(
        val catalog: List<EarnTokenWithCurrency>,
        val query: String,
        val expectedSymbols: List<String>,
    )

    private fun provideTestModels() = listOf(
        MatchingModel(
            catalog = listOf(
                earnToken(symbol = "ETHX", name = "Stader ETHx", apy = "0.9"),
                earnToken(symbol = "ETH", name = "Ethereum", apy = "0.01"),
                earnToken(symbol = "WETH", name = "Wrapped Ether", apy = "0.5"),
            ),
            query = "eth",
            // exact symbol first, then the symbol prefix, then a plain contains — yield never overtakes rank
            expectedSymbols = listOf("ETH", "ETHX", "WETH"),
        ),
        MatchingModel(
            catalog = listOf(
                earnToken(symbol = "XCOS", name = "Wrapped Cosmos", apy = "0.9"),
                earnToken(symbol = "ATOM", name = "Cosmos Hub", apy = "0.01"),
            ),
            query = "cos",
            // a name prefix outranks a name that merely contains the query
            expectedSymbols = listOf("ATOM", "XCOS"),
        ),
        MatchingModel(
            catalog = listOf(
                earnToken(symbol = "SOL", name = "Solana", apy = "0.04"),
                earnToken(symbol = "JITOSOL", name = "Jito Staked SOL", apy = "0.08"),
                earnToken(symbol = "MSOL", name = "Marinade SOL", apy = "0.06"),
            ),
            query = "sol",
            // same rank for the two contains-matches, so the higher yield comes first
            expectedSymbols = listOf("SOL", "JITOSOL", "MSOL"),
        ),
        MatchingModel(
            catalog = listOf(
                earnToken(symbol = "AAA", name = "Alpha", apy = "not-a-number"),
                earnToken(symbol = "AAB", name = "Alpha Two", apy = "0.01"),
            ),
            query = "alpha",
            // an unparseable apy sorts as zero rather than dropping the row
            expectedSymbols = listOf("AAB", "AAA"),
        ),
        MatchingModel(
            catalog = listOf(earnToken(symbol = "BTC", name = "Bitcoin")),
            query = "BiTcO",
            expectedSymbols = listOf("BTC"),
        ),
        MatchingModel(
            catalog = listOf(earnToken(symbol = "BTC", name = "Bitcoin")),
            query = "  btc  ",
            expectedSymbols = listOf("BTC"),
        ),
        MatchingModel(
            catalog = listOf(earnToken(symbol = "BTC", name = "Bitcoin")),
            query = "dogecoin",
            expectedSymbols = emptyList(),
        ),
        MatchingModel(
            catalog = listOf(earnToken(symbol = "BTC", name = "Bitcoin")),
            query = "   ",
            expectedSymbols = emptyList(),
        ),
    )

    private fun earnToken(
        symbol: String,
        name: String,
        apy: String = "0.05",
        type: EarnType = EarnType.STAKING,
    ): EarnTokenWithCurrency = createEarnTokenWithCurrency(
        earnToken = createEarnToken(apy = apy, tokenSymbol = symbol, tokenName = name, type = type),
    )
}