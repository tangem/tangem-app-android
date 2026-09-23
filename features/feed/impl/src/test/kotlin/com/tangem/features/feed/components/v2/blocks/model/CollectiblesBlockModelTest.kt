package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.CollectiblesStartRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesBlockModelTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true)
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()
    private val featureToggles: CollectiblesFeatureToggles = mockk()

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
            singleAccountListSupplier,
            portfolioSelectorController,
            portfolioFetcherFactory,
            featureToggles,
        )
        every { featureToggles.isCollectiblesEnabled } returns true
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        selection = MutableStateFlow(null)
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns selection
        // the real controller clears its pick, and the model relies on that to see the next one
        every { portfolioSelectorController.selectAccount(accountId = null) } answers { selection.value = null }
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId = any()) } returns null
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
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId = walletAId) } returns
            accountList(walletAId, Account.Collectibles(walletAId))
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
    fun `GIVEN one unlocked wallet without a collectibles account WHEN the block is tapped THEN onboarding opens`() =
        runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA)
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId = walletAId) } returns
                accountList(walletAId, mockk<Account.Personal>())
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
    fun `GIVEN the account list cannot be read WHEN the block is tapped THEN onboarding opens`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(walletA)
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId = walletAId) } returns null
        val model = createModel(testScope = this)

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
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId = walletBId) } returns
            accountList(walletBId, Account.Collectibles(walletBId))
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

    private fun createModel(testScope: TestScope): CollectiblesBlockModel = CollectiblesBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        userWalletsListRepository = userWalletsListRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        portfolioSelectorController = portfolioSelectorController,
        portfolioFetcherFactory = portfolioFetcherFactory,
        featureToggles = featureToggles,
    )

    private fun accountList(userWalletId: UserWalletId, vararg accounts: Account): AccountList = mockk {
        every { this@mockk.userWalletId } returns userWalletId
        every { this@mockk.accounts } returns accounts.toList()
    }

    private fun mockWallet(
        id: UserWalletId,
        isLocked: Boolean,
        isMultiCurrency: Boolean = true,
    ): UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns id
        every { this@mockk.isLocked } returns isLocked
        every { this@mockk.isMultiCurrency } returns isMultiCurrency
    }

}