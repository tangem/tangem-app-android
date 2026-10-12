package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PredictionDepositAddressWriter
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RefreshPredictionDepositRequisitesInteractorTest {

    private val bridgeRepository: PolymarketBridgeRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val requisitesStore: PredictionDepositRequisitesStore = mockk(relaxUnitFun = true)
    private val depositAddressWriter: PredictionDepositAddressWriter = mockk(relaxUnitFun = true)

    private val interactor = RefreshPredictionDepositRequisitesInteractor(
        bridgeRepository = bridgeRepository,
        deriveAddresses = deriveAddresses,
        requisitesStore = requisitesStore,
        depositAddressWriter = depositAddressWriter,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeRepository, deriveAddresses, requisitesStore, depositAddressWriter)
        coEvery { deriveAddresses.stored(userWalletId = USER_WALLET_ID) } returns POLYMARKET_ADDRESSES
    }

    @Test
    fun `GIVEN an unknown chain id WHEN refreshed THEN that network is dropped`() = runTest {
        // Arrange
        stubBridge(assets = listOf(asset(chainId = "137"), asset(chainId = "324", chainName = "zkSync")))

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.networks?.map { it.chainId }).containsExactly("137")
    }

    @Test
    fun `GIVEN no address for a family WHEN refreshed THEN that network is dropped`() = runTest {
        // Arrange
        stubBridge(
            assets = listOf(asset(chainId = "137"), asset(chainId = "8253038", chainName = "Bitcoin")),
            addresses = BRIDGE_ADDRESSES.copy(btc = null),
        )

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.networks?.map { it.chainId }).containsExactly("137")
    }

    @Test
    fun `GIVEN two tokens on one chain WHEN refreshed THEN they share one network row`() = runTest {
        // Arrange
        stubBridge(
            assets = listOf(
                asset(chainId = "137", tokenAddress = USDC, minCheckoutUsd = BigDecimal("2")),
                asset(chainId = "137", tokenAddress = OTHER, minCheckoutUsd = BigDecimal("9")),
            ),
        )

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        val network = actual.getOrNull()?.networks?.single()
        assertThat(network?.tokens?.map { it.minCheckoutUsd })
            .containsExactly(BigDecimal("2"), BigDecimal("9")).inOrder()
    }

    @Test
    fun `GIVEN a token without a minimum WHEN refreshed THEN it is kept without one`() = runTest {
        // Arrange
        stubBridge(
            assets = listOf(
                asset(chainId = "137", tokenAddress = USDC, minCheckoutUsd = BigDecimal("2")),
                asset(chainId = "137", tokenAddress = OTHER, minCheckoutUsd = null),
            ),
        )

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        val network = actual.getOrNull()?.networks?.single()
        assertThat(network?.tokens?.map { it.address }).containsExactly(USDC, OTHER).inOrder()
        assertThat(network?.tokens?.map { it.minCheckoutUsd }).containsExactly(BigDecimal("2"), null).inOrder()
    }

    @Test
    fun `GIVEN polygon usdc has no minimum WHEN refreshed THEN it still is the default target`() = runTest {
        // Arrange
        stubBridge(assets = listOf(asset(chainId = "137", tokenAddress = USDC, minCheckoutUsd = null)))

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.default?.address).isEqualTo(EVM_ADDRESS)
    }

    @Test
    fun `GIVEN both bridge calls are slow WHEN refreshed THEN they run concurrently`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } coAnswers {
            delay(CALL_DURATION_MS)
            listOf(asset(chainId = "137")).right()
        }
        coEvery { bridgeRepository.createDepositAddresses(any(), any()) } coAnswers {
            delay(CALL_DURATION_MS)
            BRIDGE_ADDRESSES.right()
        }

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual.isRight()).isTrue()
        assertThat(testScheduler.currentTime).isEqualTo(CALL_DURATION_MS)
    }

    @Test
    fun `GIVEN the catalogue fails at once WHEN the address call is slow THEN the failure is not delayed`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns PolymarketBridgeError.Network.left()
        coEvery { bridgeRepository.createDepositAddresses(any(), any()) } coAnswers {
            delay(CALL_DURATION_MS)
            BRIDGE_ADDRESSES.right()
        }

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketBridgeError.Network.left())
        assertThat(testScheduler.currentTime).isEqualTo(0)
    }

    @Test
    fun `GIVEN the address call fails at once WHEN the catalogue is slow THEN the failure is not delayed`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } coAnswers {
            delay(CALL_DURATION_MS)
            listOf(asset(chainId = "137")).right()
        }
        coEvery { bridgeRepository.createDepositAddresses(any(), any()) } returns PolymarketBridgeError.Network.left()

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketBridgeError.Network.left())
        assertThat(testScheduler.currentTime).isEqualTo(0)
    }

    @Test
    fun `GIVEN a successful refresh WHEN it completes THEN both stores are written`() = runTest {
        // Arrange
        stubBridge(assets = listOf(asset(chainId = "137", tokenAddress = USDC)))

        // Act
        interactor(userWalletId = USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { requisitesStore.store(userWalletId = USER_WALLET_ID, value = any()) }
        coVerify(exactly = 1) { depositAddressWriter.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS) }
    }

    @Test
    fun `GIVEN a successful refresh WHEN it completes THEN the address is written before the requisites`() =
        runTest {
            // Arrange
            stubBridge(assets = listOf(asset(chainId = "137", tokenAddress = USDC)))

            // Act
            interactor(userWalletId = USER_WALLET_ID)

            // Assert
            coVerifyOrder {
                depositAddressWriter.store(userWalletId = USER_WALLET_ID, address = EVM_ADDRESS)
                requisitesStore.store(userWalletId = USER_WALLET_ID, value = any())
            }
        }

    @Test
    fun `GIVEN polygon usdc no longer listed WHEN refreshed THEN the stored address is withdrawn`() = runTest {
        // Arrange
        stubBridge(assets = listOf(asset(chainId = "8453", chainName = "Base", tokenAddress = OTHER)))

        // Act
        interactor(userWalletId = USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { depositAddressWriter.store(userWalletId = USER_WALLET_ID, address = null) }
    }

    @Test
    fun `GIVEN the catalogue call failed WHEN refreshed THEN nothing is stored`() = runTest {
        // Arrange
        coEvery { bridgeRepository.getSupportedAssets() } returns PolymarketBridgeError.Network.left()
        coEvery { bridgeRepository.createDepositAddresses(any(), any()) } returns BRIDGE_ADDRESSES.right()

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketBridgeError.Network.left())
        coVerify(exactly = 0) { requisitesStore.store(any(), any()) }
        coVerify(exactly = 0) { depositAddressWriter.store(any(), any()) }
    }

    @Test
    fun `GIVEN no stored polymarket addresses WHEN refreshed THEN it fails without asking the bridge`() = runTest {
        // Arrange
        coEvery { deriveAddresses.stored(userWalletId = USER_WALLET_ID) } returns null
        coEvery { bridgeRepository.getSupportedAssets() } returns emptyList<PolymarketBridgeAsset>().right()

        // Act
        val actual = interactor(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketBridgeError.Unavailable.left())
        coVerify(exactly = 0) { bridgeRepository.getSupportedAssets() }
        coVerify(exactly = 0) { bridgeRepository.createDepositAddresses(any(), any()) }
    }

    private fun asset(
        chainId: String,
        chainName: String = "Polygon",
        tokenSymbol: String = "USDC",
        tokenAddress: String = USDC,
        decimals: Int = 6,
        minCheckoutUsd: BigDecimal? = BigDecimal("2"),
    ) = PolymarketBridgeAsset(
        chainId = chainId,
        chainName = chainName,
        tokenSymbol = tokenSymbol,
        tokenAddress = tokenAddress,
        decimals = decimals,
        minCheckoutUsd = minCheckoutUsd,
    )

    private fun stubBridge(
        assets: List<PolymarketBridgeAsset>,
        addresses: PolymarketBridgeAddresses = BRIDGE_ADDRESSES,
    ) {
        coEvery { bridgeRepository.getSupportedAssets() } returns assets.right()
        coEvery {
            bridgeRepository.createDepositAddresses(
                walletId = USER_WALLET_ID.stringValue,
                depositWalletAddress = POLYMARKET_ADDRESSES.depositWalletAddress,
            )
        } returns addresses.right()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        const val EVM_ADDRESS = "0x4038A7a1"
        val USDC = PolymarketContracts.USDC_TOKEN_ADDRESS
        const val OTHER = "0x1bfd67037b42cf73acf2047067bd4f2c47d9bfd6"
        const val DEPOSIT_WALLET_ADDRESS = "0xdeposit"
        const val CALL_DURATION_MS = 1_000L

        val BRIDGE_ADDRESSES = PolymarketBridgeAddresses(
            evm = EVM_ADDRESS,
            svm = "8MhRono",
            btc = "bc1q9wsh",
            tron = "TMSM4Sy",
        )

        val POLYMARKET_ADDRESSES = PolymarketAddresses(
            ownerAddress = "0x1111111111111111111111111111111111111111",
            depositWalletAddress = DEPOSIT_WALLET_ADDRESS,
            userWalletId = USER_WALLET_ID,
        )
    }
}