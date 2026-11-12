package com.tangem.data.yield.supply.status

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.yieldsupply.providers.YieldSupplyStatus
import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldModuleStatusFetcherTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val store: YieldModuleStatusStore = mockk(relaxUnitFun = true)
    private val walletManager: WalletManager = mockk()

    private val fetcher = DefaultYieldModuleStatusFetcher(
        walletManagersFacade = walletManagersFacade,
        store = store,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")
    private val usdc = createToken()
    private val usdt = createToken(contractAddress = USDT_ADDRESS, symbol = "USDT")

    @BeforeEach
    fun setUp() {
        clearMocks(walletManagersFacade, store, walletManager)

        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) } returns walletManager
        coEvery { walletManager.isSupported() } returns true
        coEvery { walletManager.getYieldModuleAddress() } returns MODULE_ADDRESS
        coEvery { walletManager.isAllowedToSpend(any()) } returns true
        coEvery { store.getSync(any()) } returns emptySet()
    }

    @Test
    fun `GIVEN no tokens WHEN invoke THEN returns right without reading anything`() = runTest {
        // Act
        val actual = fetcher(params(tokens = emptySet()))

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 0) { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) }
    }

    @Test
    fun `GIVEN no Yield Module WHEN invoke THEN every token is stored as inactive without status reads`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldModuleAddress() } returns EthereumUtils.ZERO_ADDRESS

        // Act
        val actual = fetcher(params(tokens = setOf(usdc, usdt)))

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify { store.refresh(userWalletId, setOf(createStatus(usdc).key, createStatus(usdt).key)) }
        coVerify { store.storeActual(userWalletId, setOf(createStatus(usdc), createStatus(usdt))) }
        coVerify(exactly = 0) { walletManager.getYieldSupplyStatus(any()) }
    }

    @Test
    fun `GIVEN zero module address AND stored active status WHEN invoke THEN returns left and keeps the status`() =
        runTest {
            // Arrange
            coEvery { walletManager.getYieldModuleAddress() } returns EthereumUtils.ZERO_ADDRESS
            coEvery { store.getSync(userWalletId) } returns setOf(createStatus(token = usdc, isActive = true))

            // Act
            val actual = fetcher(params(tokens = setOf(usdc, usdt)))

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify { store.storeError(userWalletId, setOf(createStatus(usdc).key, createStatus(usdt).key)) }
            coVerify(exactly = 0) { store.storeActual(any(), any()) }
        }

    @Test
    fun `GIVEN zero module address AND active status of another token on the network WHEN invoke THEN returns left and keeps the statuses`() =
        runTest {
            // Arrange
            coEvery { walletManager.getYieldModuleAddress() } returns EthereumUtils.ZERO_ADDRESS
            coEvery { store.getSync(userWalletId) } returns setOf(createStatus(token = usdt, isActive = true))

            // Act
            val actual = fetcher(params(tokens = setOf(usdc)))

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify { store.storeError(userWalletId, setOf(createStatus(usdc).key)) }
            coVerify(exactly = 0) { store.storeActual(any(), any()) }
        }

    @Test
    fun `GIVEN zero module address AND active status on another network WHEN invoke THEN tokens are stored as inactive`() =
        runTest {
            // Arrange
            val polygonUsdt = createToken(network = createNetwork(rawId = "polygon"), contractAddress = USDT_ADDRESS)
            coEvery { walletManager.getYieldModuleAddress() } returns EthereumUtils.ZERO_ADDRESS
            coEvery { store.getSync(userWalletId) } returns setOf(createStatus(token = polygonUsdt, isActive = true))

            // Act
            val actual = fetcher(params(tokens = setOf(usdc)))

            // Assert
            assertThat(actual.isRight()).isTrue()
            coVerify { store.storeActual(userWalletId, setOf(createStatus(usdc))) }
        }

    @Test
    fun `GIVEN active and inactive tokens WHEN invoke THEN the active one is read as one paired balance`() =
        runTest {
            // Arrange — allowance false in the paired read, true from the standalone call: the active token must
            // take everything from the one read
            coEvery { walletManager.getYieldSupplyStatus(USDC_ADDRESS) } returns sdkStatus(isActive = true)
            coEvery { walletManager.getYieldSupplyStatus(USDT_ADDRESS) } returns sdkStatus(isActive = false)
            coEvery { walletManager.getBalance(yieldSupplyStatus = any(), token = any()) } returns Amount(
                value = BigDecimal("20"),
                blockchain = Blockchain.Ethereum,
                currencySymbol = "USDC",
                type = AmountType.TokenYieldSupply(
                    token = Token(symbol = "USDC", contractAddress = USDC_ADDRESS, decimals = 6),
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = false,
                    effectiveProtocolBalance = BigDecimal("12.5"),
                ),
            )

            // Act
            val actual = fetcher(params(tokens = setOf(usdc, usdt)))

            // Assert
            assertThat(actual.isRight()).isTrue()
            val expected = setOf(
                createStatus(
                    token = usdc,
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = false,
                    effectiveProtocolBalance = BigDecimal("12.5"),
                    effectiveBalance = BigDecimal("20"),
                ),
                createStatus(token = usdt, isInitialized = true, isAllowedToSpend = true),
            )
            coVerify { store.storeActual(userWalletId, expected) }
            coVerify(exactly = 1) {
                walletManager.getBalance(yieldSupplyStatus = any(), token = match { it.contractAddress == USDC_ADDRESS })
            }
            coVerify(exactly = 0) { walletManager.getEffectiveProtocolBalance(any()) }
            coVerify { store.storeError(userWalletId, emptySet()) }
        }

    @Test
    fun `GIVEN paired balance of another amount type WHEN invoke THEN that token is stored as error`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldSupplyStatus(USDC_ADDRESS) } returns sdkStatus(isActive = true)
        coEvery { walletManager.getBalance(yieldSupplyStatus = any(), token = any()) } returns Amount(
            value = BigDecimal("20"),
            blockchain = Blockchain.Ethereum,
            currencySymbol = "USDC",
            type = AmountType.Coin,
        )

        // Act
        val actual = fetcher(params(tokens = setOf(usdc)))

        // Assert
        assertThat(actual.isLeft()).isTrue()
        coVerify { store.storeError(userWalletId, setOf(createStatus(usdc).key)) }
    }

    @Test
    fun `GIVEN status read fails for one token WHEN invoke THEN returns left and only that token is stored as error`() =
        runTest {
            // Arrange
            coEvery { walletManager.getYieldSupplyStatus(USDC_ADDRESS) } returns null
            coEvery { walletManager.getYieldSupplyStatus(USDT_ADDRESS) } returns sdkStatus(isActive = false)

            // Act
            val actual = fetcher(params(tokens = setOf(usdc, usdt)))

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify {
                store.storeActual(userWalletId, setOf(createStatus(usdt, isInitialized = true, isAllowedToSpend = true)))
            }
            coVerify(exactly = 1) { store.storeError(userWalletId, setOf(createStatus(usdc).key)) }
            coVerify(exactly = 0) { store.storeError(userWalletId, setOf(createStatus(usdc).key, createStatus(usdt).key)) }
        }

    @Test
    fun `GIVEN status read fails for every token WHEN invoke THEN returns left and nothing is stored as actual`() =
        runTest {
            // Arrange
            coEvery { walletManager.getYieldSupplyStatus(any()) } returns null

            // Act
            val actual = fetcher(params(tokens = setOf(usdc, usdt)))

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify { store.storeError(userWalletId, setOf(createStatus(usdc).key, createStatus(usdt).key)) }
            coVerify(exactly = 0) { store.storeActual(userWalletId, match { it.isNotEmpty() }) }
        }

    @Test
    fun `GIVEN no wallet manager WHEN invoke THEN returns right and marks stored statuses as error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) } returns null

        // Act
        val actual = fetcher(params(tokens = setOf(usdc, usdt)))

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify { store.storeError(userWalletId, setOf(createStatus(usdc).key, createStatus(usdt).key)) }
        coVerify(exactly = 0) { store.refresh(any(), any()) }
        coVerify(exactly = 0) { store.storeActual(any(), any()) }
    }

    @Test
    fun `GIVEN network without yield support WHEN invoke THEN nothing is stored`() = runTest {
        // Arrange
        coEvery { walletManager.isSupported() } returns false

        // Act
        val actual = fetcher(params(tokens = setOf(usdc)))

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 0) { store.refresh(any(), any()) }
        coVerify(exactly = 0) { store.storeActual(any(), any()) }
        coVerify(exactly = 0) { store.storeError(any(), any()) }
    }

    @Test
    fun `GIVEN one network fails WHEN invoke THEN the other network is still stored and left is returned`() =
        runTest {
            // Arrange
            val polygonUsdt = createToken(network = createNetwork(rawId = "polygon"), contractAddress = USDT_ADDRESS)
            val polygonWalletManager: WalletManager = mockk()
            coEvery { walletManagersFacade.getOrCreateWalletManager(any(), usdc.network) } returns walletManager
            coEvery {
                walletManagersFacade.getOrCreateWalletManager(any(), polygonUsdt.network)
            } returns polygonWalletManager
            coEvery { polygonWalletManager.isSupported() } throws IllegalStateException("rpc")
            coEvery { walletManager.getYieldSupplyStatus(USDC_ADDRESS) } returns sdkStatus(isActive = false)

            // Act
            val actual = fetcher(params(tokens = setOf(usdc, polygonUsdt)))

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify {
                store.storeActual(userWalletId, setOf(createStatus(usdc, isInitialized = true, isAllowedToSpend = true)))
            }
            coVerify { store.storeError(userWalletId, setOf(createStatus(polygonUsdt).key)) }
        }

    @Test
    fun `GIVEN a wrapped cancellation while the refresh is active WHEN invoke THEN returns left and stores error`() =
        runTest {
            // Arrange
            coEvery {
                walletManager.getYieldSupplyStatus(any())
            } throws IllegalStateException("wrapped", CancellationException("timeout"))

            // Act
            val actual = fetcher(params(tokens = setOf(usdc)))

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify { store.storeError(userWalletId, setOf(createStatus(usdc).key)) }
        }

    @Test
    fun `GIVEN the refresh is cancelled during a read WHEN invoke THEN no error is stored`() = runTest {
        // Arrange
        coEvery { walletManager.getYieldSupplyStatus(any()) } coAnswers { awaitCancellation() }
        val fetch = launch { fetcher(params(tokens = setOf(usdc))) }
        runCurrent()

        // Act
        fetch.cancelAndJoin()

        // Assert
        coVerify(exactly = 0) { store.storeError(any(), any()) }
    }

    private fun params(tokens: Set<CryptoCurrency.Token>) =
        YieldModuleStatusFetcher.Params(userWalletId = userWalletId, tokens = tokens)

    private fun sdkStatus(isActive: Boolean) = YieldSupplyStatus(
        isInitialized = true,
        isActive = isActive,
        maxNetworkFee = BigDecimal.ONE,
    )

    private companion object {
        const val MODULE_ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}