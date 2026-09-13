package com.tangem.features.polymarket.impl.entry.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val portfolioSelectorController: PortfolioSelectorController = mockk()
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()

    private lateinit var isEnabledFlow: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>

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
            portfolioSelectorController,
            portfolioFetcherFactory,
        )
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns MutableStateFlow(null)
        isEnabledFlow = MutableStateFlow { _: UserWallet, _: AccountStatus -> true }
        every { portfolioSelectorController.isEnabled } returns isEnabledFlow
    }

    @Test
    fun `GIVEN an ineligible wallet WHEN the selector is shown THEN it cannot be picked`() = runTest {
        // Arrange
        val ineligible = mockk<UserWallet.Cold> {
            every { walletId } returns UserWalletId("cc")
            every { isLocked } returns false
            every { isMultiCurrency } returns false
        }
        coEvery { getEligibleWalletsUseCase() } returns listOf(walletA, walletB)
        every { getEligibleWalletsUseCase.isEligible(any()) } answers { firstArg<UserWallet>().isMultiCurrency }

        // Act
        val model = createModel(testScope = this, userWalletId = null)
        advanceUntilIdle()

        // Assert
        val isEnabled = isEnabledFlow.value
        assertThat(isEnabled(ineligible, accountStatusB)).isFalse()
        assertThat(isEnabled(walletA, accountStatusB)).isTrue()
        model.onDestroy()
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

    /**
     * Predictions used to demand the deposit chain in the portfolio before it would onboard a wallet. The
     * owner key is derived on a hardened path that belongs to no network, so nothing about the portfolio can
     * hold onboarding back any more — this fails if a precondition is reintroduced.
     */
    @Test
    fun `GIVEN a settled wallet WHEN settled THEN it is handed over whatever its portfolio holds`() = runTest {
        // Arrange
        coEvery { getEligibleWalletsUseCase() } returns listOf(walletA)

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

    private fun createModel(testScope: TestScope, userWalletId: UserWalletId?): PolymarketEntryModel =
        PolymarketEntryModel(
            paramsContainer = MutableParamsContainer(PolymarketComponent.Params(userWalletId = userWalletId)),
            router = router,
            getEligibleWalletsUseCase = getEligibleWalletsUseCase,
            portfolioSelectorController = portfolioSelectorController,
            portfolioFetcherFactory = portfolioFetcherFactory,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        )

    private fun mockWallet(id: UserWalletId): UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns id
        every { isLocked } returns false
        every { isMultiCurrency } returns true
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