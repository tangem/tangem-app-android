package com.tangem.data.polymarket

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.core.error.DataError
import com.tangem.data.polymarket.store.DefaultPolymarketBridgeAssetsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.grow.datasource.polymarket.bridge.PolymarketBridgeApi
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketBridgeAddressesDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketWithdrawAddressRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketWithdrawAddressResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultPolymarketBridgeRepositoryWithdrawTest {

    private val bridgeApi: PolymarketBridgeApi = mockk()
    private val polymarketRepository: PolymarketRepository = mockk()

    private val repository = DefaultPolymarketBridgeRepository(
        bridgeApi = bridgeApi,
        polymarketRepository = polymarketRepository,
        assetsStore = DefaultPolymarketBridgeAssetsStore(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeApi, polymarketRepository)
        coEvery { polymarketRepository.getBuilderCode(any()) } returns BUILDER_CODE.right()
    }

    @Test
    fun `GIVEN a nested response WHEN creating a withdraw address THEN the evm address is returned`() = runTest {
        // Arrange
        coEvery { bridgeApi.createWithdrawAddress(any(), any()) } returns ApiResponse.Success(
            PolymarketWithdrawAddressResponse(
                nested = PolymarketBridgeAddressesDto(evm = EVM_ADDRESS, svm = "svm", btc = "btc", tron = "tron"),
            ),
        )

        // Act
        val actual = repository.createWithdrawAddress(
            walletId = WALLET_ID,
            depositWalletAddress = DEPOSIT_WALLET,
            toChainId = BASE_CHAIN_ID,
            toTokenAddress = BASE_USDC,
            recipientAddress = RECIPIENT,
        )

        // Assert
        assertThat(actual).isEqualTo(EVM_ADDRESS.right())
    }

    @Test
    fun `GIVEN a flat response WHEN creating a withdraw address THEN the evm address is returned`() = runTest {
        // Arrange
        coEvery { bridgeApi.createWithdrawAddress(any(), any()) } returns ApiResponse.Success(
            PolymarketWithdrawAddressResponse(evm = EVM_ADDRESS),
        )

        // Act
        val actual = repository.createWithdrawAddress(
            walletId = WALLET_ID,
            depositWalletAddress = DEPOSIT_WALLET,
            toChainId = BASE_CHAIN_ID,
            toTokenAddress = BASE_USDC,
            recipientAddress = RECIPIENT,
        )

        // Assert
        assertThat(actual).isEqualTo(EVM_ADDRESS.right())
    }

    @Test
    fun `GIVEN a withdrawal WHEN creating an address THEN the builder code and the destination are sent`() = runTest {
        // Arrange
        val header = slot<String>()
        val body = slot<PolymarketWithdrawAddressRequest>()
        coEvery { bridgeApi.createWithdrawAddress(capture(header), capture(body)) } returns ApiResponse.Success(
            PolymarketWithdrawAddressResponse(evm = EVM_ADDRESS),
        )

        // Act
        repository.createWithdrawAddress(
            walletId = WALLET_ID,
            depositWalletAddress = DEPOSIT_WALLET,
            toChainId = BASE_CHAIN_ID,
            toTokenAddress = BASE_USDC,
            recipientAddress = RECIPIENT,
        )

        // Assert
        assertThat(header.captured).isEqualTo(BUILDER_CODE)
        assertThat(body.captured).isEqualTo(
            PolymarketWithdrawAddressRequest(
                address = DEPOSIT_WALLET,
                toChainId = BASE_CHAIN_ID,
                toTokenAddress = BASE_USDC,
                recipientAddr = RECIPIENT,
            ),
        )
        coVerify(exactly = 1) { polymarketRepository.getBuilderCode(WALLET_ID) }
    }

    @Test
    fun `GIVEN no builder code WHEN creating a withdraw address THEN the bridge is not asked`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getBuilderCode(WALLET_ID) } returns
            DataError.NetworkError.NoInternetConnection.left()

        // Act
        val actual = repository.createWithdrawAddress(
            walletId = WALLET_ID,
            depositWalletAddress = DEPOSIT_WALLET,
            toChainId = BASE_CHAIN_ID,
            toTokenAddress = BASE_USDC,
            recipientAddress = RECIPIENT,
        )

        // Assert
        assertThat(actual).isEqualTo(PolymarketBridgeError.Unavailable.left())
        coVerify(exactly = 0) { bridgeApi.createWithdrawAddress(any(), any()) }
    }

    private companion object {
        const val WALLET_ID = "a1b2"
        const val BUILDER_CODE = "0x0000000000000000000000000000000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xdef00000000000000000000000000000000000002"
        const val EVM_ADDRESS = "0x5f3b000000000000000000000000000000000c1a2"
        const val RECIPIENT = "0x9a1b0000000000000000000000000000000007f30"
        const val BASE_CHAIN_ID = "8453"
        const val BASE_USDC = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913"
    }
}