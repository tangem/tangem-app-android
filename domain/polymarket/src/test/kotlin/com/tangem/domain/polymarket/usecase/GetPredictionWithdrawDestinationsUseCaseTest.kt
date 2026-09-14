package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PredictionWithdrawDestination
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
    fun `GIVEN supported assets and a portfolio WHEN invoke THEN every deliverable currency is a destination`(
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

    @Suppress("LongMethod")
    private fun provideTestModels(): List<MatchModel> {
        val usdcPolygon = token(POLYGON_USDC_ID, "polygon-pos", USDC_POLYGON_CHECKSUMMED)
        val usdcEthereum = token(ETHEREUM_USDC_ID, "ethereum", USDC_ETHEREUM)
        val otherPolygonToken = token(POLYGON_OTHER_ID, "polygon-pos", "0x0000000000000000000000000000000000000001")
        val unknownNetworkToken = token(UNKNOWN_NETWORK_ID, "not-a-network", USDC_POLYGON_CHECKSUMMED)
        val polygonCoin = coin(POLYGON_COIN_ID, "polygon-pos")
        val ethereumClassicCoin = coin(ETHEREUM_CLASSIC_COIN_ID, "ethereum-classic")
        val usdcSolana = token(SOLANA_USDC_ID, "solana", USDC_SOLANA_MINT)
        val usdcSolanaLowercased = token(SOLANA_OTHER_ID, "solana", USDC_SOLANA_MINT.lowercase())
        val usdtTron = token(TRON_USDT_ID, "tron", USDT_TRON)
        val bitcoinCoin = coin(BITCOIN_COIN_ID, "bitcoin")

        return listOf(
            MatchModel(
                supported = listOf(asset(POLYGON_CHAIN_ID, "Polygon", USDC_POLYGON_CHECKSUMMED.lowercase())),
                portfolio = listOf(usdcPolygon),
                expected = mapOf(
                    POLYGON_USDC_ID to destination(
                        POLYGON_USDC_ID,
                        POLYGON_CHAIN_ID,
                        USDC_POLYGON_CHECKSUMMED.lowercase(),
                    ),
                ),
            ),
            MatchModel(
                supported = listOf(asset(POLYGON_CHAIN_ID, "Polygon", NATIVE_ASSET_ADDRESS)),
                portfolio = listOf(polygonCoin, ethereumClassicCoin),
                expected = mapOf(
                    POLYGON_COIN_ID to destination(POLYGON_COIN_ID, POLYGON_CHAIN_ID, NATIVE_ASSET_ADDRESS),
                ),
            ),
            MatchModel(
                supported = listOf(asset(SOLANA_CHAIN_ID, "Solana", USDC_SOLANA_MINT)),
                portfolio = listOf(usdcSolana, usdcSolanaLowercased),
                expected = mapOf(
                    SOLANA_USDC_ID to destination(SOLANA_USDC_ID, SOLANA_CHAIN_ID, USDC_SOLANA_MINT),
                ),
            ),
            MatchModel(
                supported = listOf(
                    asset(TRON_CHAIN_ID, "Tron", USDT_TRON),
                    asset(BITCOIN_CHAIN_ID, "Bitcoin", NATIVE_ASSET_ADDRESS),
                ),
                portfolio = listOf(usdtTron, bitcoinCoin),
                expected = mapOf(
                    TRON_USDT_ID to destination(TRON_USDT_ID, TRON_CHAIN_ID, USDT_TRON),
                    BITCOIN_COIN_ID to destination(BITCOIN_COIN_ID, BITCOIN_CHAIN_ID, NATIVE_ASSET_ADDRESS),
                ),
            ),
            MatchModel(
                supported = listOf(
                    asset(POLYGON_CHAIN_ID, "Polygon", USDC_POLYGON_CHECKSUMMED.lowercase()),
                    asset(ETHEREUM_CHAIN_ID, "Ethereum", USDC_ETHEREUM),
                ),
                portfolio = listOf(usdcPolygon, usdcEthereum, otherPolygonToken, unknownNetworkToken),
                expected = mapOf(
                    POLYGON_USDC_ID to destination(
                        POLYGON_USDC_ID,
                        POLYGON_CHAIN_ID,
                        USDC_POLYGON_CHECKSUMMED.lowercase(),
                    ),
                    ETHEREUM_USDC_ID to destination(ETHEREUM_USDC_ID, ETHEREUM_CHAIN_ID, USDC_ETHEREUM),
                ),
            ),
            MatchModel(
                supported = listOf(asset(INK_CHAIN_ID, "Ink", NATIVE_ASSET_ADDRESS)),
                portfolio = listOf(usdcPolygon, polygonCoin),
                expected = emptyMap(),
            ),
            MatchModel(
                supported = emptyList(),
                portfolio = listOf(usdcPolygon),
                expected = emptyMap(),
            ),
        )
    }

    data class MatchModel(
        val supported: List<PolymarketBridgeAsset>,
        val portfolio: List<CryptoCurrency>,
        val expected: Map<CryptoCurrency.ID, PredictionWithdrawDestination>,
    )

    private fun token(id: CryptoCurrency.ID, networkRawId: String, contract: String): CryptoCurrency.Token {
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { network } returns mockk(relaxed = true) { every { rawId } returns networkRawId }
            every { contractAddress } returns contract
        }
    }

    private fun coin(id: CryptoCurrency.ID, networkRawId: String): CryptoCurrency.Coin {
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { network } returns mockk(relaxed = true) { every { rawId } returns networkRawId }
        }
    }

    private fun destination(id: CryptoCurrency.ID, chainId: String, tokenAddress: String) =
        PredictionWithdrawDestination(currencyId = id, chainId = chainId, tokenAddress = tokenAddress)

    private fun asset(chainId: String, chainName: String, tokenAddress: String) = PolymarketBridgeAsset(
        chainId = chainId,
        chainName = chainName,
        tokenSymbol = "USDC",
        tokenAddress = tokenAddress,
        decimals = 6,
        minCheckoutUsd = BigDecimal("2"),
    )

    private companion object {
        const val POLYGON_CHAIN_ID = "137"
        const val ETHEREUM_CHAIN_ID = "1"
        const val INK_CHAIN_ID = "57073"
        const val SOLANA_CHAIN_ID = "1151111081099710"
        const val BITCOIN_CHAIN_ID = "8253038"
        const val TRON_CHAIN_ID = "728126428"

        const val USDC_POLYGON_CHECKSUMMED = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val USDC_ETHEREUM = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val USDC_SOLANA_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        const val USDT_TRON = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val NATIVE_ASSET_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"

        val POLYGON_USDC_ID: CryptoCurrency.ID = mockk()
        val ETHEREUM_USDC_ID: CryptoCurrency.ID = mockk()
        val POLYGON_OTHER_ID: CryptoCurrency.ID = mockk()
        val UNKNOWN_NETWORK_ID: CryptoCurrency.ID = mockk()
        val POLYGON_COIN_ID: CryptoCurrency.ID = mockk()
        val ETHEREUM_CLASSIC_COIN_ID: CryptoCurrency.ID = mockk()
        val SOLANA_USDC_ID: CryptoCurrency.ID = mockk()
        val SOLANA_OTHER_ID: CryptoCurrency.ID = mockk()
        val TRON_USDT_ID: CryptoCurrency.ID = mockk()
        val BITCOIN_COIN_ID: CryptoCurrency.ID = mockk()
    }
}