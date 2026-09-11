package com.tangem.data.polymarket

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.polymarket.store.DefaultPolymarketBridgeAssetsStore
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.grow.datasource.polymarket.bridge.PolymarketBridgeApi
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketBridgeAddressesDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositResponse
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketBridgeTokenDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetsResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultPolymarketBridgeRepositoryTest {

    private val bridgeApi: PolymarketBridgeApi = mockk()
    private val polymarketRepository: PolymarketRepository = mockk()

    private lateinit var repository: DefaultPolymarketBridgeRepository

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeApi, polymarketRepository)
        repository = DefaultPolymarketBridgeRepository(
            bridgeApi = bridgeApi,
            polymarketRepository = polymarketRepository,
            assetsStore = DefaultPolymarketBridgeAssetsStore(),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    @Test
    fun `GIVEN a builder code WHEN deposit addresses are created THEN it is sent as a header`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getBuilderCode(any()) } returns Either.Right(BUILDER_CODE)
        coEvery { bridgeApi.createDepositAddresses(any(), any()) } returns ApiResponse.Success(
            PolymarketDepositResponse(
                address = PolymarketBridgeAddressesDto(evm = BRIDGE_ADDRESS, svm = "sol", btc = "bc1", tron = null),
            ),
        )

        // Act
        val actual = repository.createDepositAddresses(walletId = WALLET_ID, depositWalletAddress = DEPOSIT_WALLET)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(
                PolymarketBridgeAddresses(evm = BRIDGE_ADDRESS, svm = "sol", btc = "bc1", tron = null),
            ),
        )
        coVerify(exactly = 1) {
            bridgeApi.createDepositAddresses(BUILDER_CODE, PolymarketDepositRequest(address = DEPOSIT_WALLET))
        }
    }

    @Test
    fun `GIVEN the builder code is unavailable WHEN addresses are created THEN the bridge is not called`() =
        runTest {
            // Arrange
            coEvery { polymarketRepository.getBuilderCode(any()) } returns Either.Left(
                DataError.NetworkError.NoInternetConnection,
            )

            // Act
            val actual = repository.createDepositAddresses(
                walletId = WALLET_ID,
                depositWalletAddress = DEPOSIT_WALLET,
            )

            // Assert
            assertThat(actual).isEqualTo(Either.Left(PolymarketBridgeError.Unavailable))
            coVerify(exactly = 0) { bridgeApi.createDepositAddresses(any(), any()) }
        }

    @Test
    fun `GIVEN only the evm family is served WHEN addresses are created THEN the others stay absent`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getBuilderCode(any()) } returns Either.Right(BUILDER_CODE)
        coEvery { bridgeApi.createDepositAddresses(any(), any()) } returns ApiResponse.Success(
            PolymarketDepositResponse(
                address = PolymarketBridgeAddressesDto(evm = BRIDGE_ADDRESS, svm = null, btc = null, tron = null),
            ),
        )

        // Act
        val actual = repository.createDepositAddresses(walletId = WALLET_ID, depositWalletAddress = DEPOSIT_WALLET)

        // Assert
        val addresses = actual.getOrNull()!!
        assertThat(addresses.evm).isEqualTo(BRIDGE_ADDRESS)
        assertThat(addresses.svm).isNull()
        assertThat(addresses.btc).isNull()
        assertThat(addresses.tron).isNull()
    }

    @Test
    fun `GIVEN the bridge is unreachable WHEN assets are listed THEN Network is reported`() = runTest {
        // Arrange
        coEvery { bridgeApi.getSupportedAssets() } returns networkError()

        // Act
        val actual = repository.getSupportedAssets()

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketBridgeError.Network))
    }

    @Test
    fun `GIVEN assets are served WHEN listed THEN the minimum keeps the digits the bridge sent`() = runTest {
        // Arrange
        coEvery { bridgeApi.getSupportedAssets() } returns ApiResponse.Success(
            PolymarketSupportedAssetsResponse(
                supportedAssets = listOf(
                    PolymarketSupportedAssetDto(
                        chainId = "137",
                        chainName = "Polygon",
                        token = PolymarketBridgeTokenDto(
                            name = "USD Coin",
                            symbol = "USDC",
                            address = "0x3c49",
                            decimals = 6,
                        ),
                        minCheckoutUsd = BigDecimal("2.50"),
                    ),
                ),
            ),
        )

        // Act
        val actual = repository.getSupportedAssets()

        // Assert
        val asset = actual.getOrNull()!!.single()
        assertThat(asset.chainId).isEqualTo("137")
        assertThat(asset.chainName).isEqualTo("Polygon")
        assertThat(asset.tokenSymbol).isEqualTo("USDC")
        assertThat(asset.tokenAddress).isEqualTo("0x3c49")
        assertThat(asset.decimals).isEqualTo(6)
        assertThat(asset.minCheckoutUsd).isEqualTo(BigDecimal("2.50"))
    }

    @Test
    fun `GIVEN catalogue already fetched WHEN asked again THEN the api is called once`() = runTest {
        // Arrange
        coEvery { bridgeApi.getSupportedAssets() } returns responseWithOneAsset()

        // Act
        repository.getSupportedAssets()
        repository.getSupportedAssets()

        // Assert
        coVerify(exactly = 1) { bridgeApi.getSupportedAssets() }
    }

    @Test
    fun `GIVEN catalogue call failed WHEN asked again THEN the api is called again`() = runTest {
        // Arrange
        coEvery { bridgeApi.getSupportedAssets() } returnsMany listOf(networkError(), responseWithOneAsset())

        // Act
        val first = repository.getSupportedAssets()
        val second = repository.getSupportedAssets()

        // Assert
        assertThat(first).isEqualTo(Either.Left(PolymarketBridgeError.Network))
        assertThat(second.getOrNull()).hasSize(1)
        coVerify(exactly = 2) { bridgeApi.getSupportedAssets() }
    }

    @Test
    fun `GIVEN an asset without a minimum WHEN listed THEN the minimum is absent`() = runTest {
        // Arrange
        coEvery { bridgeApi.getSupportedAssets() } returns ApiResponse.Success(
            PolymarketSupportedAssetsResponse(
                supportedAssets = listOf(
                    PolymarketSupportedAssetDto(
                        chainId = "1",
                        chainName = "Bitcoin",
                        token = PolymarketBridgeTokenDto(name = "Bitcoin", symbol = "BTC", address = "", decimals = 8),
                        minCheckoutUsd = null,
                    ),
                ),
            ),
        )

        // Act
        val actual = repository.getSupportedAssets()

        // Assert
        assertThat(actual.getOrNull()!!.single().minCheckoutUsd).isNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> networkError(): ApiResponse<T> =
        ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<T>

    private fun responseWithOneAsset() = ApiResponse.Success(
        PolymarketSupportedAssetsResponse(
            supportedAssets = listOf(
                PolymarketSupportedAssetDto(
                    chainId = "137",
                    chainName = "Polygon",
                    token = PolymarketBridgeTokenDto(
                        name = "USD Coin",
                        symbol = "USDC",
                        address = "0x3c49",
                        decimals = 6,
                    ),
                    minCheckoutUsd = BigDecimal("2.50"),
                ),
            ),
        ),
    )

    private companion object {
        const val WALLET_ID = "7CE25DC32EF792CFC32380007A4172F5B64F67E4F91D37F14B351A76DAFA33DA"
        const val BUILDER_CODE = "0x0000000000000000000000000000000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xDEf0000000000000000000000000000000000002"
        const val BRIDGE_ADDRESS = "0x5f3b000000000000000000000000000000000003"
    }
}