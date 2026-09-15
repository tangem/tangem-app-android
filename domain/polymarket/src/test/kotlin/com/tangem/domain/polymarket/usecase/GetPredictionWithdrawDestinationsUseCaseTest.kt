package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
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
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory = mockk()

    private val useCase = GetPredictionWithdrawDestinationsUseCase(
        bridgeRepository = bridgeRepository,
        collateralCurrencyFactory = collateralCurrencyFactory,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeRepository, collateralCurrencyFactory)
        every { collateralCurrencyFactory.createDepositTarget() } returns depositTarget()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN supported assets and a portfolio WHEN invoke THEN only USDC on Polygon the bridge lists is a destination`(
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
    fun `GIVEN USDC on Polygon the bridge does not list WHEN resolved THEN the map is empty`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns
            listOf(asset(INK_CHAIN_ID, "Ink", NATIVE_ASSET_ADDRESS)).right()

        // Act
        val actual = useCase(portfolio = listOf(token(POLYGON_USDC_ID, "polygon-pos", USDC_POLYGON_CHECKSUMMED)))

        // Assert
        assertThat(actual).isEqualTo(emptyMap<CryptoCurrency.ID, PredictionWithdrawDestination>().right())
    }

    @Test
    fun `GIVEN USDC on Polygon held in two wallets WHEN resolved THEN both are destinations with the bridge's chain id and token address`() =
        runTest {
            // Arrange
            coEvery { bridgeRepository.getSupportedAssets() } returns
                listOf(asset(POLYGON_CHAIN_ID, "Polygon", USDC_POLYGON_CHECKSUMMED.lowercase())).right()
            val firstWallet = token(POLYGON_USDC_ID, "polygon-pos", USDC_POLYGON_CHECKSUMMED)
            val secondWallet = token(POLYGON_USDC_ID_OTHER_WALLET, "polygon-pos", USDC_POLYGON_CHECKSUMMED)

            // Act
            val actual = useCase(portfolio = listOf(firstWallet, secondWallet))

            // Assert
            assertThat(actual).isEqualTo(
                mapOf(
                    POLYGON_USDC_ID to destination(POLYGON_USDC_ID, POLYGON_CHAIN_ID, USDC_POLYGON_CHECKSUMMED.lowercase()),
                    POLYGON_USDC_ID_OTHER_WALLET to destination(
                        POLYGON_USDC_ID_OTHER_WALLET,
                        POLYGON_CHAIN_ID,
                        USDC_POLYGON_CHECKSUMMED.lowercase(),
                    ),
                ).right(),
            )
        }

    @Test
    fun `GIVEN an upper-cased USDC Polygon contract address WHEN resolved THEN it still matches the deposit target`() =
        runTest {
            // Arrange
            coEvery { bridgeRepository.getSupportedAssets() } returns
                listOf(asset(POLYGON_CHAIN_ID, "Polygon", USDC_POLYGON_CHECKSUMMED.lowercase())).right()
            val upperCased = token(POLYGON_USDC_ID, "polygon-pos", USDC_POLYGON_CHECKSUMMED.uppercase())

            // Act
            val actual = useCase(portfolio = listOf(upperCased))

            // Assert
            assertThat(actual).isEqualTo(
                mapOf(
                    POLYGON_USDC_ID to destination(POLYGON_USDC_ID, POLYGON_CHAIN_ID, USDC_POLYGON_CHECKSUMMED.lowercase()),
                ).right(),
            )
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
                name = "GIVEN USDC on Polygon bridge-listed WHEN resolved THEN it is a destination",
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
                name = "GIVEN native Polygon and Ethereum Classic coins WHEN resolved THEN neither is a destination for now",
                supported = listOf(asset(POLYGON_CHAIN_ID, "Polygon", NATIVE_ASSET_ADDRESS)),
                portfolio = listOf(polygonCoin, ethereumClassicCoin),
                expected = emptyMap(),
            ),
            MatchModel(
                name = "GIVEN a bridge-listed Solana USDC WHEN resolved THEN it is not a destination for now",
                supported = listOf(asset(SOLANA_CHAIN_ID, "Solana", USDC_SOLANA_MINT)),
                portfolio = listOf(usdcSolana, usdcSolanaLowercased),
                expected = emptyMap(),
            ),
            MatchModel(
                name = "GIVEN bridge-listed Tron USDT and Bitcoin WHEN resolved THEN neither is a destination for now",
                supported = listOf(
                    asset(TRON_CHAIN_ID, "Tron", USDT_TRON),
                    asset(BITCOIN_CHAIN_ID, "Bitcoin", NATIVE_ASSET_ADDRESS),
                ),
                portfolio = listOf(usdtTron, bitcoinCoin),
                expected = emptyMap(),
            ),
            MatchModel(
                name = "GIVEN USDC on Polygon and other EVM tokens WHEN resolved THEN only USDC on Polygon is a destination",
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
                ),
            ),
            MatchModel(
                name = "GIVEN the bridge lists no assets WHEN resolved THEN the map is empty",
                supported = emptyList(),
                portfolio = listOf(usdcPolygon),
                expected = emptyMap(),
            ),
        )
    }

    data class MatchModel(
        val name: String,
        val supported: List<PolymarketBridgeAsset>,
        val portfolio: List<CryptoCurrency>,
        val expected: Map<CryptoCurrency.ID, PredictionWithdrawDestination>,
    ) {
        override fun toString(): String = name
    }

    private fun depositTarget(): CryptoCurrency.Token = token(
        id = mockk(),
        networkRawId = "polygon-pos",
        contract = USDC_POLYGON_CHECKSUMMED.lowercase(),
        decimals = 6,
    )

    private fun token(
        id: CryptoCurrency.ID,
        networkRawId: String,
        contract: String,
        decimals: Int = 6,
    ): CryptoCurrency.Token {
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { network } returns mockk(relaxed = true) { every { rawId } returns networkRawId }
            every { contractAddress } returns contract
            every { this@mockk.decimals } returns decimals
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
        val POLYGON_USDC_ID_OTHER_WALLET: CryptoCurrency.ID = mockk()
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