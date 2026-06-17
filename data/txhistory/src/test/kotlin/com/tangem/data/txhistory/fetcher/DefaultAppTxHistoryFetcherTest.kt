package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAppTxHistoryFetcherTest {

    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val selectedWalletUseCase: GetSelectedWalletUseCase = mockk()
    private val walletFetcherFactory: DefaultWalletTxHistoryFetcher.Factory = mockk()
    private val expressRepository: ExpressRepository = mockk()

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun setup() {
        clearMocks(getWalletsUseCase, selectedWalletUseCase, walletFetcherFactory, expressRepository)
        every { selectedWalletUseCase.selectedFlow() } returns emptyFlow()
        coEvery { expressRepository.getProviders(any(), any()) } returns emptyList()
    }

    @Test
    fun `creates wallet fetcher for each new wallet`() = runTest {
        val utils = createUtils()
        val walletsFlow = MutableStateFlow(linkedMapOf<UserWalletId, UserWallet>())
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns walletsFlow
        val walletFetcher1 = relaxedWalletFetcher()
        every { walletFetcherFactory.create(WALLET_ID_1) } returns walletFetcher1

        val fetcher = createFetcher(utils)
        advanceUntilIdle()
        assertThat(fetcher.fetchers).isEmpty()

        // Act
        walletsFlow.value = linkedMapOf(WALLET_ID_1 to mockk())
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.fetchers.keys).containsExactly(WALLET_ID_1)
        verify(exactly = 1) { walletFetcherFactory.create(WALLET_ID_1) }
    }

    @Test
    fun `closes and removes fetcher when wallet is removed`() = runTest {
        val utils = createUtils()
        val walletsFlow = MutableStateFlow(
            linkedMapOf<UserWalletId, UserWallet>(WALLET_ID_1 to mockk(), WALLET_ID_2 to mockk()),
        )
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns walletsFlow
        val walletFetcher1 = relaxedWalletFetcher()
        val walletFetcher2 = relaxedWalletFetcher()
        every { walletFetcherFactory.create(WALLET_ID_1) } returns walletFetcher1
        every { walletFetcherFactory.create(WALLET_ID_2) } returns walletFetcher2

        val fetcher = createFetcher(utils)
        advanceUntilIdle()
        assertThat(fetcher.fetchers.keys).containsExactly(WALLET_ID_1, WALLET_ID_2)

        // Act
        walletsFlow.value = linkedMapOf(WALLET_ID_1 to mockk())
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.fetchers.keys).containsExactly(WALLET_ID_1)
        verify(exactly = 1) { walletFetcher2.close() }
        verify(inverse = true) { walletFetcher1.close() }
    }

    @Test
    fun `routes trigger to the fetcher of the target wallet`() = runTest {
        val utils = createUtils()
        val walletsFlow = MutableStateFlow(linkedMapOf<UserWalletId, UserWallet>(WALLET_ID_1 to mockk()))
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns walletsFlow
        val walletFetcher1 = relaxedWalletFetcher()
        every { walletFetcherFactory.create(WALLET_ID_1) } returns walletFetcher1

        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID_1, currency = currency)
        fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { walletFetcher1.invoke(trigger) }
    }

    @Test
    fun `does nothing when trigger targets unknown wallet`() = runTest {
        val utils = createUtils()
        val walletsFlow = MutableStateFlow(linkedMapOf<UserWalletId, UserWallet>())
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns walletsFlow

        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Act
        val trigger = TxHistoryFetchTrigger.TokenDetailsOpen(walletId = WALLET_ID_1, currency = currency)
        val result = fetcher.invoke(trigger)
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.fetchers).isEmpty()
        verify(inverse = true) { walletFetcherFactory.create(any()) }
    }

    @Test
    fun `close cancels scope and closes all child fetchers`() = runTest {
        val utils = createUtils()
        val walletsFlow = MutableStateFlow(linkedMapOf<UserWalletId, UserWallet>(WALLET_ID_1 to mockk()))
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns walletsFlow
        val walletFetcher1 = relaxedWalletFetcher()
        every { walletFetcherFactory.create(WALLET_ID_1) } returns walletFetcher1

        val fetcher = createFetcher(utils)
        advanceUntilIdle()
        assertThat(fetcher.fetchers.keys).containsExactly(WALLET_ID_1)

        // Act
        fetcher.close()

        // Assert
        assertThat(fetcher.fetchers).isEmpty()
        verify(exactly = 1) { walletFetcher1.close() }
        assertThat(utils.fetcherScope.coroutineContext.job.isActive).isFalse()
    }

    @Test
    fun `loads express providers when selected wallet is multi-currency`() = runTest {
        // Arrange
        val utils = createUtils()
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns MutableStateFlow(linkedMapOf())
        val wallet = mockk<UserWallet.Cold>(relaxed = true) {
            every { isMultiCurrency } returns true
            every { walletId } returns WALLET_ID_1
        }
        every { selectedWalletUseCase.selectedFlow() } returns flowOf(wallet)

        // Act
        createFetcher(utils)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { expressRepository.getProviders(wallet, emptyList()) }
    }

    @Test
    fun `loads express providers only once per wallet`() = runTest {
        // Arrange
        val utils = createUtils()
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns MutableStateFlow(linkedMapOf())
        val wallet = mockk<UserWallet.Cold>(relaxed = true) {
            every { isMultiCurrency } returns true
            every { walletId } returns WALLET_ID_1
        }
        // Same wallet selected several times.
        every { selectedWalletUseCase.selectedFlow() } returns flowOf(wallet, wallet, wallet)

        // Act
        createFetcher(utils)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { expressRepository.getProviders(wallet, emptyList()) }
    }

    @Test
    fun `provider loading failure does not break the wallet pipeline`() = runTest {
        // Arrange
        val utils = createUtils()
        val walletsFlow = MutableStateFlow(linkedMapOf<UserWalletId, UserWallet>())
        every { getWalletsUseCase.invokeAsMap(any(), any()) } returns walletsFlow
        val wallet = mockk<UserWallet.Cold>(relaxed = true) {
            every { isMultiCurrency } returns true
            every { walletId } returns WALLET_ID_1
        }
        every { selectedWalletUseCase.selectedFlow() } returns flowOf(wallet)
        coEvery { expressRepository.getProviders(any(), any()) } throws RuntimeException("boom")
        val walletFetcher1 = relaxedWalletFetcher()
        every { walletFetcherFactory.create(WALLET_ID_1) } returns walletFetcher1

        val fetcher = createFetcher(utils)
        advanceUntilIdle()

        // Act — the provider error is swallowed, so the wallet pipeline must keep working.
        walletsFlow.value = linkedMapOf(WALLET_ID_1 to mockk())
        advanceUntilIdle()

        // Assert
        assertThat(fetcher.fetchers.keys).containsExactly(WALLET_ID_1)
    }

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(utils: DefaultTxHistoryFetcherUtils) = DefaultAppTxHistoryFetcher(
        utils = utils,
        expressRepository = expressRepository,
        getWalletsUseCase = getWalletsUseCase,
        selectedWalletUseCase = selectedWalletUseCase,
        walletTxHistoryFetcherFactory = walletFetcherFactory,
    )

    private fun relaxedWalletFetcher() = mockk<DefaultWalletTxHistoryFetcher>(relaxed = true)

    private companion object {
        val WALLET_ID_1 = UserWalletId("001")
        val WALLET_ID_2 = UserWalletId("002")
    }
}