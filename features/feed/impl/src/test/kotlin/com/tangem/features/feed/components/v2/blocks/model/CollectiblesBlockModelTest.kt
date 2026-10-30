package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.CollectiblesStartRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.blocks.CollectiblesBlockComponent
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.feed.components.v2.trackSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesBlockModelTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true)
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()
    private val featureToggles: CollectiblesFeatureToggles = mockk()
    private val collectiblesAccountStatusSupplier: CollectiblesAccountStatusSupplier = mockk()
    private val isEnabledPredicate = MutableStateFlow<(UserWallet, AccountStatus) -> Boolean> { _, _ -> true }

    private lateinit var selection: MutableStateFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>

    private val walletAId = UserWalletId("aa")
    private val walletBId = UserWalletId("bb")
    private val walletA: UserWallet = mockWallet(id = walletAId, isLocked = false)
    private val walletB: UserWallet = mockWallet(id = walletBId, isLocked = false)
    private val lockedWallet: UserWallet = mockWallet(id = UserWalletId("cc"), isLocked = true)
    private val accountStatus: AccountStatus.CryptoPortfolio = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            userWalletsListRepository,
            portfolioSelectorController,
            portfolioFetcherFactory,
            featureToggles,
            collectiblesAccountStatusSupplier,
        )
        every { featureToggles.isCollectiblesEnabled } returns true
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        selection = MutableStateFlow(null)
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns selection
        // the real controller clears its pick, and the model relies on that to see the next one
        every { portfolioSelectorController.selectAccount(accountId = null) } answers { selection.value = null }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(walletA, walletB))
        every { collectiblesAccountStatusSupplier(any<UserWalletId>()) } returns
            flowOf(CollectiblesAccountStatusValue.NotOnboarded)
        every { portfolioSelectorController.isEnabled } returns isEnabledPredicate
    }

    /** An old single-currency card cannot derive the vault owner key, so the account does not exist for it. */
    @Test
    fun `GIVEN only a single-currency wallet WHEN the block is tapped THEN nothing opens`() = runTest {
        // Arrange
        val singleCurrencyWallet = mockWallet(id = walletAId, isLocked = false, isMultiCurrency = false)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(singleCurrencyWallet)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { router.push(route = any(), onComplete = any()) }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the feature is off WHEN the model is created THEN the block is hidden`() = runTest {
        // Arrange
        every { featureToggles.isCollectiblesEnabled } returns false

        // Act
        val model = createModel(testScope = this)

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN one unlocked wallet with a collectibles account WHEN the block is tapped THEN main opens`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA, lockedWallet)
        every { collectiblesAccountStatusSupplier(walletAId) } returns flowOf(ONBOARDED)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(
                route = AppRoute.Collectibles(
                    userWalletId = walletAId,
                    startRoute = CollectiblesStartRoute.Main,
                ),
                onComplete = any(),
            )
        }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN one unlocked wallet not onboarded WHEN the block is tapped THEN onboarding opens`() =
        runTest {
            // Arrange — also the case of a backend record with no vault behind it
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA)
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.push(
                    route = AppRoute.Collectibles(
                        userWalletId = walletAId,
                        startRoute = CollectiblesStartRoute.Onboarding,
                    ),
                    onComplete = any(),
                )
            }
            assertThat(openedSheets).isEmpty()
            model.onDestroy()
        }

    @Test
    fun `GIVEN an account awaiting the delegate update WHEN the block is tapped THEN main opens`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA)
        every { collectiblesAccountStatusSupplier(walletAId) } returns
            flowOf(CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.CACHE))
        val model = createModel(testScope = this)

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(
                route = AppRoute.Collectibles(
                    userWalletId = walletAId,
                    startRoute = CollectiblesStartRoute.Main,
                ),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN only locked wallets WHEN the block is tapped THEN nothing opens`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(lockedWallet)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN several unlocked wallets WHEN the block is tapped THEN the selector is shown AND nothing opens`() =
        runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA, walletB)
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
            assertThat(openedSheets).containsExactly(CollectiblesBlockComponent.WalletSelectorConfig)
            model.onDestroy()
        }

    @Test
    fun `GIVEN the selector is shown WHEN a wallet is picked THEN the sheet closes AND that wallet opens`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA, walletB)
        every { collectiblesAccountStatusSupplier(walletBId) } returns flowOf(ONBOARDED)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        model.onClick()
        advanceUntilIdle()

        // Act
        selection.value = walletB to accountStatus
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(
                route = AppRoute.Collectibles(
                    userWalletId = walletBId,
                    startRoute = CollectiblesStartRoute.Main,
                ),
                onComplete = any(),
            )
        }
        assertThat(openedSheets)
            .containsExactly(CollectiblesBlockComponent.WalletSelectorConfig, null)
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a wallet was already picked once WHEN the same one is picked again THEN it opens again`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA, walletB)
        val model = createModel(testScope = this)
        model.onClick()
        advanceUntilIdle()
        selection.value = walletB to accountStatus
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()
        selection.value = walletB to accountStatus
        advanceUntilIdle()

        // Assert
        verify(exactly = 2) {
            router.push(
                route = AppRoute.Collectibles(
                    userWalletId = walletBId,
                    startRoute = CollectiblesStartRoute.Onboarding,
                ),
                onComplete = any(),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the selector is dismissed without a pick WHEN dismiss is invoked THEN nothing opens`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA, walletB)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        model.onClick()
        advanceUntilIdle()

        // Act
        model.portfolioSelectorCallback.onDismiss()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        assertThat(openedSheets)
            .containsExactly(CollectiblesBlockComponent.WalletSelectorConfig, null)
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN every wallet's account awaits the address sync WHEN the model is created THEN the block is hidden`() =
        runTest {
            // Arrange
            every { collectiblesAccountStatusSupplier(any<UserWalletId>()) } returns
                flowOf(CollectiblesAccountStatusValue.MissedDerivation)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN one wallet's account awaits the address sync WHEN the model is created THEN the block is shown`() =
        runTest {
            // Arrange
            every { collectiblesAccountStatusSupplier(walletAId) } returns
                flowOf(CollectiblesAccountStatusValue.MissedDerivation)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN the only wallet's account awaits the address sync WHEN the block is tapped THEN nothing opens`() =
        runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA)
            every { collectiblesAccountStatusSupplier(walletAId) } returns
                flowOf(CollectiblesAccountStatusValue.MissedDerivation)
            val model = createModel(testScope = this)

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(inverse = true) { router.push(route = any(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN a wallet awaits the address sync WHEN the selector is shown THEN that wallet is disabled`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA, walletB)
        every { collectiblesAccountStatusSupplier(walletAId) } returns
            flowOf(CollectiblesAccountStatusValue.MissedDerivation)
        val model = createModel(testScope = this)

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        val isEnabled = isEnabledPredicate.value
        assertThat(isEnabled(walletA, accountStatus)).isFalse()
        assertThat(isEnabled(walletB, accountStatus)).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN only locked wallets WHEN the model is created THEN the block stays shown`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(lockedWallet))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN every account awaits the address sync WHEN one gets synced THEN the block appears`() = runTest {
        // Arrange
        val statusA = MutableStateFlow<CollectiblesAccountStatusValue>(CollectiblesAccountStatusValue.MissedDerivation)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(walletA))
        every { collectiblesAccountStatusSupplier(walletAId) } returns statusA
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val hidden = model.isVisible.value

        // Act
        statusA.value = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
        advanceUntilIdle()

        // Assert
        assertThat(hidden).isFalse()
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): CollectiblesBlockModel = CollectiblesBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        userWalletsListRepository = userWalletsListRepository,
        collectiblesAccountStatusSupplier = collectiblesAccountStatusSupplier,
        portfolioSelectorController = portfolioSelectorController,
        portfolioFetcherFactory = portfolioFetcherFactory,
        featureToggles = featureToggles,
    )

    private fun mockWallet(
        id: UserWalletId,
        isLocked: Boolean,
        isMultiCurrency: Boolean = true,
    ): UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns id
        every { this@mockk.isLocked } returns isLocked
        every { this@mockk.isMultiCurrency } returns isMultiCurrency
    }

    private companion object {
        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE)
    }
}