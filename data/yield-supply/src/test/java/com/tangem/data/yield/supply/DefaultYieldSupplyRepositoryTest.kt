package com.tangem.data.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.WalletManager
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.data.yield.supply.converters.YieldMarketTokenConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import com.tangem.datasource.api.tangemTech.models.YieldModuleStatusResponse
import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import com.tangem.datasource.api.tangemTech.models.YieldTokenChartResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldSupplyRepositoryTest {

    private val yieldSupplyApi: YieldSupplyApi = mockk()
    private val store: YieldMarketsStore = mockk(relaxed = true)
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)
    private val appPreferencesStore: AppPreferencesStore = mockk()

    private val repository = DefaultYieldSupplyRepository(
        yieldSupplyApi = yieldSupplyApi,
        store = store,
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsExceptionHandler = analyticsExceptionHandler,
        appPreferencesStore = appPreferencesStore,
    )

    private val userWalletId = UserWalletId("abcdef012345")
    private val token = token()

    @BeforeEach
    fun setUp() {
        clearMocks(yieldSupplyApi, store, walletManagersFacade, analyticsExceptionHandler)
    }

    // region markets
    @Test
    fun `GIVEN cached dtos WHEN getCachedMarkets THEN returns enriched domain`() = runTest {
        // Arrange
        coEvery { store.getSyncOrNull() } returns listOf(marketDto(chainId = 1))

        // Act
        val result = repository.getCachedMarkets()

        // Assert — chainId 1 is enriched to its network id
        assertThat(result).hasSize(1)
        assertThat(result.first().backendId).isEqualTo("ethereum")
    }

    @Test
    fun `GIVEN empty cache WHEN getCachedMarkets THEN returns empty list`() = runTest {
        // Arrange
        coEvery { store.getSyncOrNull() } returns null

        // Act
        val result = repository.getCachedMarkets()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN cached dto with unmapped chain id WHEN getCachedMarkets THEN backend id is null`() = runTest {
        // Arrange — chainId -1 (the converter's default for a DTO without a chainId) maps to no network
        coEvery { store.getSyncOrNull() } returns listOf(marketDto(chainId = -1))

        // Act
        val result = repository.getCachedMarkets()

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result.first().backendId).isNull()
    }

    @Test
    fun `GIVEN api returns markets WHEN updateMarkets THEN stores dtos and returns domain`() = runTest {
        // Arrange
        val dto = marketDto(chainId = 1)
        coEvery { yieldSupplyApi.getYieldMarkets(any()) } returns ApiResponse.Success(
            YieldMarketsResponse(marketDtos = listOf(dto), lastUpdated = "now"),
        )

        // Act
        val result = repository.updateMarkets()

        // Assert
        assertThat(result).containsExactly(YieldMarketTokenConverter.convert(dto))
        coVerify(exactly = 1) { store.store(listOf(dto)) }
    }

    @Test
    fun `GIVEN store flow WHEN getMarketsFlow THEN emits enriched domain`() = runTest {
        // Arrange
        every { store.get() } returns flowOf(listOf(marketDto(chainId = 1)))

        // Act
        val result = repository.getMarketsFlow().first()

        // Assert
        assertThat(result.first().backendId).isEqualTo("ethereum")
    }
    // endregion

    // region token status / chart
    @Test
    fun `GIVEN evm token WHEN getTokenStatus THEN returns converted market token`() = runTest {
        // Arrange
        val dto = marketDto(chainId = 1)
        coEvery { yieldSupplyApi.getYieldTokenStatus(1, token.contractAddress) } returns ApiResponse.Success(dto)

        // Act
        val result = repository.getTokenStatus(token)

        // Assert
        assertThat(result).isEqualTo(YieldMarketTokenConverter.convert(dto))
    }

    @Test
    fun `GIVEN non-evm token WHEN getTokenStatus THEN throws`() = runTest {
        // Arrange
        val nonEvm = token(rawId = "unknown-network-xyz")

        // Act
        val error = runCatching { repository.getTokenStatus(nonEvm) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN evm token WHEN getTokenChart THEN returns converted chart`() = runTest {
        // Arrange
        coEvery { yieldSupplyApi.getYieldTokenChart(1, token.contractAddress) } returns ApiResponse.Success(
            chartResponse(),
        )

        // Act
        val result = repository.getTokenChart(token)

        // Assert
        assertThat(result.avr).isEqualTo(4.25)
        assertThat(result.y).containsExactly(3.5).inOrder()
    }

    @Test
    fun `GIVEN non-evm token WHEN getTokenChart THEN throws`() = runTest {
        // Arrange
        val nonEvm = token(rawId = "unknown-network-xyz")

        // Act
        val error = runCatching { repository.getTokenChart(nonEvm) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
    // endregion

    // region isYieldSupplySupported
    @Test
    fun `GIVEN supported yield provider WHEN isYieldSupplySupported THEN returns true`() = runTest {
        // Arrange — WalletManager itself implements YieldSupplyProvider
        val walletManager = mockk<WalletManager> { every { isSupported() } returns true }
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) } returns walletManager

        // Act
        val result = repository.isYieldSupplySupported(userWalletId, token)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN unsupported yield provider WHEN isYieldSupplySupported THEN returns false`() = runTest {
        // Arrange
        val walletManager = mockk<WalletManager> { every { isSupported() } returns false }
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) } returns walletManager

        // Act
        val result = repository.isYieldSupplySupported(userWalletId, token)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN no wallet manager WHEN isYieldSupplySupported THEN sends analytics and returns false`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any(), any()) } returns null

        // Act
        val result = repository.isYieldSupplySupported(userWalletId, token)

        // Assert
        assertThat(result).isFalse()
        verify { analyticsExceptionHandler.sendException(any()) }
    }
    // endregion

    // region activate / deactivate
    @Test
    fun `GIVEN api returns active WHEN activateProtocol THEN returns true`() = runTest {
        // Arrange
        coEvery {
            yieldSupplyApi.activateYieldModule(body = any(), userWalletId = any())
        } returns ApiResponse.Success(statusResponse(isActive = true))

        // Act
        val result = repository.activateProtocol(userWalletId, token, ADDRESS)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN non-evm token WHEN activateProtocol THEN throws`() = runTest {
        // Arrange
        val nonEvm = token(rawId = "unknown-network-xyz")

        // Act
        val error = runCatching { repository.activateProtocol(userWalletId, nonEvm, ADDRESS) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN api returns inactive WHEN deactivateProtocol THEN returns false`() = runTest {
        // Arrange
        coEvery { yieldSupplyApi.deactivateYieldModule(any()) } returns ApiResponse.Success(
            statusResponse(isActive = false),
        )

        // Act
        val result = repository.deactivateProtocol(token, ADDRESS)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN non-evm token WHEN deactivateProtocol THEN throws`() = runTest {
        // Arrange
        val nonEvm = token(rawId = "unknown-network-xyz")

        // Act
        val error = runCatching { repository.deactivateProtocol(nonEvm, ADDRESS) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
    // endregion

    // region pending status (in-memory)
    @Test
    fun `GIVEN saved pending status WHEN getTokenProtocolPendingStatus THEN returns it`() = runTest {
        // Arrange
        val status = YieldSupplyPendingStatus.Enter(txIds = listOf("0x1"), createdAt = 1L)
        repository.saveTokenProtocolPendingStatus(userWalletId, token, status)

        // Act
        val result = repository.getTokenProtocolPendingStatus(userWalletId, token)

        // Assert
        assertThat(result).isEqualTo(status)
    }

    @Test
    fun `GIVEN saved then cleared WHEN getTokenProtocolPendingStatus THEN returns null`() = runTest {
        // Arrange
        repository.saveTokenProtocolPendingStatus(
            userWalletId,
            token,
            YieldSupplyPendingStatus.Enter(txIds = listOf("0x1"), createdAt = 1L),
        )

        // Act
        repository.saveTokenProtocolPendingStatus(userWalletId, token, null)
        val result = repository.getTokenProtocolPendingStatus(userWalletId, token)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN saved status WHEN flow collected THEN emits the status`() = runTest {
        // Arrange
        val status = YieldSupplyPendingStatus.Exit(txIds = listOf("0x9"), createdAt = 1L)
        repository.saveTokenProtocolPendingStatus(userWalletId, token, status)

        // Act
        val emitted = repository.getTokenProtocolPendingStatusFlow(userWalletId, token).first()

        // Assert
        assertThat(emitted).isEqualTo(status)
    }
    // endregion

    // region pending tx hashes
    @Test
    fun `GIVEN unconfirmed and confirmed txs WHEN getPendingTxHashes THEN returns only unconfirmed hashes`() = runTest {
        // Arrange
        val walletManager = mockk<WalletManager> {
            every { wallet.recentTransactions } returns mutableListOf(
                tx(TransactionStatus.Unconfirmed, "0xUnconfirmed"),
                tx(TransactionStatus.Confirmed, "0xConfirmed"),
            )
        }
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) } returns walletManager

        // Act
        val result = repository.getPendingTxHashes(userWalletId, token)

        // Assert
        assertThat(result).containsExactly("0xUnconfirmed")
    }

    @Test
    fun `GIVEN no wallet manager WHEN getPendingTxHashes THEN returns empty`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) } returns null

        // Act
        val result = repository.getPendingTxHashes(userWalletId, token)

        // Assert
        assertThat(result).isEmpty()
    }
    // endregion

    // region promo banner preference
    @Test
    fun `GIVEN stored flag WHEN getShouldShowYieldPromoBanner THEN emits it`() = runTest {
        // Arrange
        mockkStatic("com.tangem.datasource.local.preferences.utils.PreferencesDataStoreExtKt")
        try {
            every {
                appPreferencesStore.get(PreferencesKeys.YIELD_SUPPLY_SHOULD_SHOW_MAIN_PROMO_KEY, true)
            } returns flowOf(false)

            // Act
            val result = repository.getShouldShowYieldPromoBanner().first()

            // Assert
            assertThat(result).isFalse()
        } finally {
            unmockkStatic("com.tangem.datasource.local.preferences.utils.PreferencesDataStoreExtKt")
        }
    }

    @Test
    fun `WHEN setShouldShowYieldPromoBanner THEN stores the value`() = runTest {
        // Arrange
        mockkStatic("com.tangem.datasource.local.preferences.utils.PreferencesDataStoreExtKt")
        try {
            coEvery {
                appPreferencesStore.store(PreferencesKeys.YIELD_SUPPLY_SHOULD_SHOW_MAIN_PROMO_KEY, false)
            } returns Unit

            // Act
            repository.setShouldShowYieldPromoBanner(false)

            // Assert
            coVerify { appPreferencesStore.store(PreferencesKeys.YIELD_SUPPLY_SHOULD_SHOW_MAIN_PROMO_KEY, false) }
        } finally {
            unmockkStatic("com.tangem.datasource.local.preferences.utils.PreferencesDataStoreExtKt")
        }
    }
    // endregion

    private fun tx(status: TransactionStatus, hash: String): TransactionData.Uncompiled = mockk {
        every { this@mockk.status } returns status
        every { this@mockk.hash } returns hash
    }

    private fun marketDto(chainId: Int) = YieldSupplyMarketTokenDto(
        tokenAddress = "0xToken",
        tokenSymbol = "USDT",
        tokenName = "Tether",
        apy = BigDecimal("5.5"),
        decimals = 6,
        isActive = true,
        chainId = chainId,
        maxFeeNative = BigDecimal("0.005"),
        maxFeeUSD = BigDecimal("12.34"),
    )

    private fun chartResponse() = YieldTokenChartResponse(
        underlying = "USDT",
        market = "aave",
        bucketSizeDays = 1,
        period = "30d",
        data = listOf(YieldTokenChartResponse.DataPoint(bucketIndex = 0, avgApy = BigDecimal("3.5"))),
        averageApy = BigDecimal("4.25"),
    )

    private fun statusResponse(isActive: Boolean) = YieldModuleStatusResponse(
        tokenAddress = "0xToken",
        chainId = 1,
        isActive = isActive,
        activatedAt = null,
        deactivatedAt = null,
    )

    private fun token(rawId: String = "ethereum", contractAddress: String = "0xToken"): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = rawId, derivationPath = derivationPath),
            name = "Net",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawId),
            ),
            network = network,
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }

    private companion object {
        const val ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}