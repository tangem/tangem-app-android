package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.referral.ReferralRepository
import com.tangem.domain.referral.ReferralStatus
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.blocks.ReferralBlockComponent
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.feed.components.v2.trackSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ReferralBlockModelTest {

    private val router: Router = mockk(relaxed = true)
    private val referralRepository: ReferralRepository = mockk()
    private val isDemoCardUseCase: IsDemoCardUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()

    private val walletAId = UserWalletId("aa")
    private val walletBId = UserWalletId("bb")
    private val accountStatus: AccountStatus.CryptoPortfolio = mockk()

    private lateinit var wallets: MutableStateFlow<List<UserWallet>?>
    private lateinit var selection: MutableStateFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>
    private lateinit var isWalletEnabled: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            referralRepository,
            isDemoCardUseCase,
            userWalletsListRepository,
            portfolioSelectorController,
            portfolioFetcherFactory,
        )
        mockkStatic(ScanResponse::cardTypesResolver)
        wallets = MutableStateFlow(emptyList())
        selection = MutableStateFlow(null)
        isWalletEnabled = MutableStateFlow { _, _ -> true }
        every { userWalletsListRepository.userWallets } returns wallets
        every { portfolioSelectorController.isEnabled } returns isWalletEnabled
        every { portfolioFetcherFactory.create(any(), any()) } returns mockk(relaxed = true)
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns selection
        every { portfolioSelectorController.selectAccount(accountId = null) } answers { selection.value = null }
        every { isDemoCardUseCase(any<String>()) } returns false
        coEvery { referralRepository.getReferralStatus(any()) } returns referralStatus(isActive = false)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ScanResponse::cardTypesResolver)
    }

    @Test
    fun `GIVEN a wallet that has not joined WHEN the model is created THEN the invitation is shown`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN every wallet has joined WHEN the model is created THEN the invitation is hidden`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId), hotWallet(walletBId))
        coEvery { referralRepository.getReferralStatus(any()) } returns referralStatus(isActive = true)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN one of two wallets has joined WHEN the model is created THEN the invitation is still shown`() =
        runTest {
            // Arrange
            wallets.value = listOf(hotWallet(walletAId), hotWallet(walletBId))
            coEvery { referralRepository.getReferralStatus(walletAId.stringValue) } returns
                referralStatus(isActive = true)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN the status cannot be read WHEN the model is created THEN the invitation stays visible`() = runTest {
        // Arrange — hiding it from someone eligible costs more than showing it to someone who joined
        wallets.value = listOf(hotWallet(walletAId))
        coEvery { referralRepository.getReferralStatus(any()) } throws IllegalStateException("network down")

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a locked wallet WHEN the model is created THEN it is neither asked nor offered`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId, isLocked = true))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        coVerify(exactly = 0) { referralRepository.getReferralStatus(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a demo card WHEN the model is created THEN it is neither asked nor offered`() = runTest {
        // Arrange
        wallets.value = listOf(coldWallet(walletAId, isTangemWallet = true, isDemo = true))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        coVerify(exactly = 0) { referralRepository.getReferralStatus(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a card the programme is closed to WHEN the model is created THEN it is neither asked nor offered`() =
        runTest {
            // Arrange
            wallets.value = listOf(coldWallet(walletAId, isTangemWallet = false))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            coVerify(exactly = 0) { referralRepository.getReferralStatus(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the same wallets are re-emitted WHEN the list refreshes THEN the status is not requested again`() =
        runTest {
            // Arrange
            wallets.value = listOf(hotWallet(walletAId))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act — a balance refresh re-emits the same wallets as new instances
            wallets.value = listOf(hotWallet(walletAId))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { referralRepository.getReferralStatus(walletAId.stringValue) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN one invitable wallet WHEN the block is tapped THEN the referral screen opens for it`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(route = AppRoute.ReferralProgram(userWalletId = walletAId), onComplete = any())
        }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN several invitable wallets WHEN the block is tapped THEN the chooser opens instead`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId), hotWallet(walletBId))
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        assertThat(openedSheets).containsExactly(ReferralBlockComponent.WalletSelectorConfig)
        model.onDestroy()
    }

    @Test
    fun `GIVEN the chooser is open WHEN a wallet is picked THEN the sheet closes AND that wallet opens`() = runTest {
        // Arrange
        val walletB = hotWallet(walletBId)
        wallets.value = listOf(hotWallet(walletAId), walletB)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.onClick()
        advanceUntilIdle()

        // Act
        selection.value = walletB to accountStatus
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(route = AppRoute.ReferralProgram(userWalletId = walletBId), onComplete = any())
        }
        assertThat(openedSheets)
            .containsExactly(ReferralBlockComponent.WalletSelectorConfig, null)
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the referral was opened for the only wallet WHEN it joined THEN the invitation hides on return`() =
        runTest {
            // Arrange
            wallets.value = listOf(hotWallet(walletAId))
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onClick()
            advanceUntilIdle()
            coEvery { referralRepository.getReferralStatus(walletAId.stringValue) } returns
                referralStatus(isActive = true)

            // Act
            model.onResume()
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN the referral was opened WHEN the wallet did not join THEN the invitation stays on return`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onClick()
        advanceUntilIdle()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        coVerify(exactly = 2) { referralRepository.getReferralStatus(walletAId.stringValue) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN one of two wallets joined WHEN returning THEN it is offered as a disabled row`() = runTest {
        // Arrange
        val walletA = hotWallet(walletAId)
        val walletB = hotWallet(walletBId)
        wallets.value = listOf(walletA, walletB)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.onClick()
        selection.value = walletA to accountStatus
        advanceUntilIdle()
        coEvery { referralRepository.getReferralStatus(walletAId.stringValue) } returns
            referralStatus(isActive = true)

        // Act — back from A's referral screen, where it joined
        model.onResume()
        advanceUntilIdle()
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        assertThat(isWalletEnabled.value(walletA, accountStatus)).isFalse()
        assertThat(isWalletEnabled.value(walletB, accountStatus)).isTrue()
        assertThat(openedSheets)
            .containsExactly(
                ReferralBlockComponent.WalletSelectorConfig,
                null,
                ReferralBlockComponent.WalletSelectorConfig,
            )
            .inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN one of two wallets has joined WHEN the block is tapped THEN the joined one opens disabled`() = runTest {
        // Arrange
        val walletA = hotWallet(walletAId)
        val walletB = hotWallet(walletBId)
        wallets.value = listOf(walletA, walletB)
        coEvery { referralRepository.getReferralStatus(walletBId.stringValue) } returns
            referralStatus(isActive = true)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(ReferralBlockComponent.WalletSelectorConfig)
        assertThat(isWalletEnabled.value(walletA, accountStatus)).isTrue()
        assertThat(isWalletEnabled.value(walletB, accountStatus)).isFalse()
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the join statuses are still being read WHEN the card is built THEN it shows as a skeleton`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))
        coEvery { referralRepository.getReferralStatus(any()) } coAnswers {
            delay(timeMillis = STATUS_DELAY_MS)
            referralStatus(isActive = false)
        }

        // Act
        val model = createModel(testScope = this)
        runCurrent()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        assertThat(model.isLoading.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the join statuses have been read WHEN the card is built THEN the skeleton is gone`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        assertThat(model.isLoading.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no wallet the programme is open to WHEN the card is built THEN no skeleton is shown`() = runTest {
        // Arrange
        wallets.value = listOf(coldWallet(walletAId, isTangemWallet = false))

        // Act
        val model = createModel(testScope = this)
        runCurrent()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        assertThat(model.isLoading.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the card was never tapped WHEN the feed resumes THEN no status is requested again`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { referralRepository.getReferralStatus(walletAId.stringValue) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no invitable wallet WHEN the block is tapped THEN nothing opens`() = runTest {
        // Arrange
        wallets.value = listOf(hotWallet(walletAId))
        coEvery { referralRepository.getReferralStatus(any()) } returns referralStatus(isActive = true)
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): ReferralBlockModel = ReferralBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        referralRepository = referralRepository,
        isDemoCardUseCase = isDemoCardUseCase,
        userWalletsListRepository = userWalletsListRepository,
        portfolioFetcherFactory = portfolioFetcherFactory,
        portfolioSelectorController = portfolioSelectorController,
    )

    private fun hotWallet(id: UserWalletId, isLocked: Boolean = false): UserWallet = mockk<UserWallet.Hot> {
        every { walletId } returns id
        every { this@mockk.isLocked } returns isLocked
    }

    private fun coldWallet(
        id: UserWalletId,
        isTangemWallet: Boolean,
        isDemo: Boolean = false,
    ): UserWallet {
        val resolver: CardTypesResolver = mockk { every { isTangemWallet() } returns isTangemWallet }
        val scan: ScanResponse = mockk()
        every { scan.cardTypesResolver } returns resolver
        every { isDemoCardUseCase(CARD_ID) } returns isDemo
        return mockk<UserWallet.Cold> {
            every { walletId } returns id
            every { isLocked } returns false
            every { scanResponse } returns scan
            every { cardId } returns CARD_ID
        }
    }

    private fun referralStatus(isActive: Boolean): ReferralStatus =
        ReferralStatus(isActive = isActive, token = null, address = null)

    private companion object {
        const val CARD_ID = "card-1"
        const val STATUS_DELAY_MS = 1_000L
    }
}