package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPredictionWithdrawDestinationsUseCaseTest {

    private val bridgeRepository: PolymarketBridgeRepository = mockk()

    private val useCase = GetPredictionWithdrawDestinationsUseCase(bridgeRepository = bridgeRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN supported assets and a portfolio WHEN invoke THEN only the matching tokens are destinations`(
        model: MatchModel,
    ) = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns model.supported.right()

        // Act
        val actual = useCase(portfolio = model.portfolio)

        // Assert
        assertThat(actual).isEqualTo(model.expected.right())
    }

    @Test
    fun `GIVEN the bridge fails WHEN invoke THEN the error is returned`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns PolymarketBridgeError.Network.left()

        // Act
        val actual = useCase(portfolio = listOf(token(POLYGON_USDC_ID, "polygon-pos", USDC_POLYGON_CHECKSUMMED)))

        // Assert
        assertThat(actual).isEqualTo(PolymarketBridgeError.Network.left())
    }

    private fun provideTestModels(): List<MatchModel> {
        val usdcPolygon = token(POLYGON_USDC_ID, "polygon-pos", USDC_POLYGON_CHECKSUMMED)
        val usdcEthereum = token(ETHEREUM_USDC_ID, "ethereum", USDC_ETHEREUM)
        val otherPolygonToken = token(POLYGON_OTHER_ID, "polygon-pos", "0x0000000000000000000000000000000000000001")
        val unknownNetworkToken = token(mockk(), "not-a-network", USDC_POLYGON_CHECKSUMMED)
        val polygonCoin: CryptoCurrency.Coin = mockk(relaxed = true)

        return listOf(
            MatchModel(
                supported = listOf(asset(POLYGON_CHAIN_ID, USDC_POLYGON_CHECKSUMMED.lowercase())),
                portfolio = listOf(usdcPolygon),
                expected = setOf(POLYGON_USDC_ID),
            ),
            MatchModel(
                supported = listOf(asset(POLYGON_CHAIN_ID, USDC_POLYGON_CHECKSUMMED.lowercase())),
                portfolio = listOf(usdcEthereum, otherPolygonToken, unknownNetworkToken, polygonCoin),
                expected = emptySet(),
            ),
            MatchModel(
                supported = listOf(
                    asset(POLYGON_CHAIN_ID, USDC_POLYGON_CHECKSUMMED.lowercase()),
                    asset(ETHEREUM_CHAIN_ID, USDC_ETHEREUM),
                ),
                portfolio = listOf(usdcPolygon, usdcEthereum, otherPolygonToken),
                expected = setOf(POLYGON_USDC_ID, ETHEREUM_USDC_ID),
            ),
            MatchModel(
                supported = emptyList(),
                portfolio = listOf(usdcPolygon),
                expected = emptySet(),
            ),
        )
    }

    data class MatchModel(
        val supported: List<PolymarketBridgeAsset>,
        val portfolio: List<CryptoCurrency>,
        val expected: Set<CryptoCurrency.ID>,
    )

    private fun token(id: CryptoCurrency.ID, networkRawId: String, contract: String): CryptoCurrency.Token {
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { network } returns mockk(relaxed = true) { every { rawId } returns networkRawId }
            every { contractAddress } returns contract
        }
    }

    private fun asset(chainId: String, tokenAddress: String) = PolymarketBridgeAsset(
        chainId = chainId,
        chainName = "chain",
        tokenSymbol = "USDC",
        tokenAddress = tokenAddress,
        decimals = 6,
        minCheckoutUsd = BigDecimal("2"),
    )

    private companion object {
        const val POLYGON_CHAIN_ID = "137"
        const val ETHEREUM_CHAIN_ID = "1"
        const val USDC_POLYGON_CHECKSUMMED = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val USDC_ETHEREUM = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"

        val POLYGON_USDC_ID: CryptoCurrency.ID = mockk()
        val ETHEREUM_USDC_ID: CryptoCurrency.ID = mockk()
        val POLYGON_OTHER_ID: CryptoCurrency.ID = mockk()
    }
}