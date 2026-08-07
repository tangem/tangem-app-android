package com.tangem.features.polymarket.impl.entry.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase
import com.tangem.domain.polymarket.usecase.HasPolymarketDepositNetworkUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PolymarketEntryModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getEligibleWalletsUseCase: GetPolymarketEligibleWalletsUseCase = mockk()
    private val hasDepositNetworkUseCase: HasPolymarketDepositNetworkUseCase = mockk()
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk()
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()

    private val addToPortfolioManager: AddToPortfolioManager = mockk(relaxed = true)
    private val onDismissChannel = Channel<Unit>()
    private val onSuccessAddedChannel = Channel<AddToPortfolioManager.Result>()
    private val onAddedTokenClickChannel = Channel<AddToPortfolioManager.Result>()

    private val walletAId = UserWalletId("aa")
    private val walletBId = UserWalletId("bb")
    private val walletA: UserWallet = mockWallet(id = walletAId)
    private val walletB: UserWallet = mockWallet(id = walletBId)
    private val accountStatusB: AccountStatus.CryptoPortfolio = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            getEligibleWalletsUseCase,
            hasDepositNetworkUseCase,
            addToPortfolioManagerFactory,
            portfolioSelectorController,
            portfolioFetcherFactory,
            addToPortfolioManager,
        )
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns MutableStateFlow(null)
        every { portfolioSelectorController.isEnabled } returns
            MutableStateFlow { _: UserWallet, _: AccountStatus -> true }
        coEvery { hasDepositNetworkUseCase(any()) } returns true

        every { addToPortfolioManager.onDismiss } returns onDismissChannel
        every { addToPortfolioManager.onSuccessAdded } returns onSuccessAddedChannel
        every { addToPortfolioManager.onAddedTokenClick } returns onAddedTokenClickChannel
        every { addToPortfolioManager.updateLaunchMode(any()) } just Runs
        every { addToPortfolioManager.setTokenParams(any<RawMarketToken>()) } just Runs
        every { addToPortfolioManager.setTokenNetworks(any()) } just Runs
        every { addToPortfolioManagerFactory.create(any(), any(), any()) } returns addToPortfolioManager
    }

    @Test
    fun `GIVEN caller passed a wallet AND several eligible wallets WHEN the entry route opens THEN that wallet is settled without asking`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA, walletB)

            // Act
            val model = createModel(testScope = this, userWalletId = walletAId)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Onboarding(userWalletId = walletAId)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN no wallet passed AND exactly one eligible wallet WHEN the entry route opens THEN it is auto-picked without asking`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)

            // Act
            val model = createModel(testScope = this, userWalletId = null)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Onboarding(userWalletId = walletAId)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN no wallet passed AND several eligible wallets WHEN the entry route opens THEN the selector is shown AND nothing is handed over`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA, walletB)

            // Act
            val model = createModel(testScope = this, userWalletId = null)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN no wallet passed AND no eligible wallets WHEN the entry route opens THEN the feature is popped`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns emptyList()

            // Act
            val model = createModel(testScope = this, userWalletId = null)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.pop(onComplete = any()) }
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the selector emits a pick WHEN it is observed THEN that wallet is handed over`() = runTest {
        // Arrange
        coEvery { getEligibleWalletsUseCase() } returns listOf(walletA, walletB)
        val selection = MutableStateFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>(null)
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns selection
        val model = createModel(testScope = this, userWalletId = null)
        advanceUntilIdle()

        // Act
        selection.value = walletB to accountStatusB
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Onboarding(userWalletId = walletBId)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the selector re-emits the same picked wallet WHEN a balance refresh follows it THEN hand-over happens only once`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA, walletB)
            val selection = MutableSharedFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>(replay = 1)
            every { portfolioSelectorController.selectedAccountWithData(any()) } returns selection
            val model = createModel(testScope = this, userWalletId = null)
            advanceUntilIdle()

            // Act
            selection.emit(walletB to accountStatusB)
            advanceUntilIdle()
            selection.emit(walletB to accountStatusB)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Onboarding(userWalletId = walletBId)),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the selector is dismissed without a pick WHEN dismiss is invoked THEN the feature is popped`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA, walletB)
            val model = createModel(testScope = this, userWalletId = null)
            advanceUntilIdle()

            // Act
            model.portfolioSelectorCallback.onDismiss()

            // Assert
            verify(exactly = 1) { router.pop(onComplete = any()) }
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the settled wallet already holds the deposit network WHEN settled THEN it hands over without offering to add it`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)
            coEvery { hasDepositNetworkUseCase(walletAId) } returns true

            // Act
            val model = createModel(testScope = this, userWalletId = walletAId)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    routes = arrayOf(PolymarketRoute.Onboarding(userWalletId = walletAId)),
                    onComplete = any(),
                )
            }
            verify(exactly = 0) { addToPortfolioManagerFactory.create(any(), any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the settled wallet is missing the deposit network WHEN settled THEN the add sheet is offered AND nothing is handed over`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)
            coEvery { hasDepositNetworkUseCase(walletAId) } returns false

            // Act
            val model = createModel(testScope = this, userWalletId = walletAId)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            assertThat(model.addToPortfolioManager).isNotNull()
            model.onDestroy()
        }

    @Test
    fun `GIVEN the add sheet reports success WHEN observed THEN the added wallet is handed over`() = runTest {
        // Arrange
        coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)
        coEvery { hasDepositNetworkUseCase(walletAId) } returns false
        val model = createModel(testScope = this, userWalletId = walletAId)
        advanceUntilIdle()

        // Act
        onSuccessAddedChannel.send(
            AddToPortfolioManager.Result(wallet = walletA, account = mockk(), addedCurrency = mockk()),
        )
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.replaceAll(
                routes = arrayOf(PolymarketRoute.Onboarding(userWalletId = walletAId)),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the add sheet reports success for a different wallet WHEN observed THEN nothing is handed over`() = runTest {
        // Arrange
        coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)
        coEvery { hasDepositNetworkUseCase(walletAId) } returns false
        val model = createModel(testScope = this, userWalletId = walletAId)
        advanceUntilIdle()

        // Act
        onSuccessAddedChannel.send(
            AddToPortfolioManager.Result(wallet = walletB, account = mockk(), addedCurrency = mockk()),
        )
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the deposit network is missing WHEN the add sheet is built THEN the manager is configured with Preselected`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)
            coEvery { hasDepositNetworkUseCase(walletAId) } returns false

            // Act
            val model = createModel(testScope = this, userWalletId = walletAId)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                addToPortfolioManagerFactory.create(
                    scope = any(),
                    settings = AddToPortfolioManager.Settings(shouldSkipTokenActionsScreen = true),
                    analyticsParams = any(),
                )
            }
            verify(exactly = 1) { addToPortfolioManager.updateLaunchMode(AddToPortfolioManager.LaunchMode.Preselected) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the add sheet is dismissed without adding WHEN observed THEN nothing is handed over AND the feature is not popped`() =
        runTest {
            // Arrange
            coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)
            coEvery { hasDepositNetworkUseCase(walletAId) } returns false
            val model = createModel(testScope = this, userWalletId = walletAId)
            advanceUntilIdle()

            // Act
            onDismissChannel.send(Unit)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            verify(exactly = 0) { router.pop(onComplete = any()) }
            model.onDestroy()
        }

    private fun createModel(testScope: TestScope, userWalletId: UserWalletId?): PolymarketEntryModel =
        PolymarketEntryModel(
            paramsContainer = MutableParamsContainer(PolymarketComponent.Params(userWalletId = userWalletId)),
            router = router,
            getEligibleWalletsUseCase = getEligibleWalletsUseCase,
            hasDepositNetworkUseCase = hasDepositNetworkUseCase,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
            portfolioSelectorController = portfolioSelectorController,
            portfolioFetcherFactory = portfolioFetcherFactory,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )

    private fun mockWallet(id: UserWalletId): UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns id
        every { isLocked } returns false
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}