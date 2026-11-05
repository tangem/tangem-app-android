package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.left
import arrow.core.right
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.wallets.usecase.DerivationOutcome
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkError
import com.tangem.domain.wallets.usecase.DeriveCurrencyAndOwnerKeysUseCase
import com.tangem.domain.wallets.usecase.LinkWalletToDeviceUseCase
import com.tangem.feature.wallet.link.WalletLinkAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletLinkProgressHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.hotwallet.HardwareUpgradeLauncher
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletWarningsClickIntentsTest {

    private val deriveCurrencyAndOwnerKeysUseCase: DeriveCurrencyAndOwnerKeysUseCase = mockk()
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val hardwareUpgradeLauncher: HardwareUpgradeLauncher = mockk()
    private val screenLifecycleProvider = ScreenLifecycleProvider()
    private val collectiblesAccountStatusFetcher: CollectiblesAccountStatusFetcher = mockk()
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles = mockk()
    private val stateHolder: WalletStateController = mockk(relaxUnitFun = true)
    private val linkWalletToDeviceUseCase: LinkWalletToDeviceUseCase = mockk()

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val currencies = listOf<CryptoCurrency>(mockk(relaxed = true))

    @BeforeEach
    fun setUp() {
        clearMocks(
            deriveCurrencyAndOwnerKeysUseCase,
            multiNetworkStatusFetcher,
            multiQuoteStatusFetcher,
            multiStakingBalanceFetcher,
            stakingIdFactory,
            uiMessageSender,
            analyticsEventHandler,
            hardwareUpgradeLauncher,
            collectiblesAccountStatusFetcher,
            stateHolder,
            linkWalletToDeviceUseCase,
        )
        screenLifecycleProvider.onResume()
        every { collectiblesFeatureToggles.isCollectiblesEnabled } returns true
        coEvery { collectiblesAccountStatusFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { hardwareUpgradeLauncher.launch(any(), any()) } just runs
        coEvery { multiNetworkStatusFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { multiQuoteStatusFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { multiStakingBalanceFetcher(params = any()) } returns Either.Right(Unit)
        coEvery { stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any()) } returns Either.Right(mockk())
    }

    @Test
    fun `GIVEN card derived nothing WHEN generate missed addresses THEN wrong card dialog is shown`() = runTest {
        // Arrange
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns
            Either.Right(DerivationOutcome.NOTHING_DERIVED)
        val message = slot<UiMessage>()

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat((message.captured as DialogMessage).message).isEqualTo(Dialogs.wrongWalletTapped().message)
        coVerify(exactly = 0) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiQuoteStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiStakingBalanceFetcher(params = any()) }
        coVerify(exactly = 0) { collectiblesAccountStatusFetcher(params = any()) }
    }

    @Test
    fun `GIVEN collectibles are off WHEN keys are derived THEN the collectibles account is not refreshed`() = runTest {
        // Arrange
        every { collectiblesFeatureToggles.isCollectiblesEnabled } returns false
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns
            Either.Right(DerivationOutcome.DERIVED)

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 0) { collectiblesAccountStatusFetcher(params = any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN card derived keys WHEN generate missed addresses THEN currencies are fetched`(
        outcome: DerivationOutcome,
    ) = runTest {
        // Arrange
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns Either.Right(outcome)

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 1) { multiQuoteStatusFetcher(params = any()) }
        coVerify(exactly = 1) { multiStakingBalanceFetcher(params = any()) }
        coVerify(exactly = 1) {
            collectiblesAccountStatusFetcher(CollectiblesAccountStatusFetcher.Params(userWalletId = userWalletId))
        }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN derivation failed WHEN generate missed addresses THEN nothing is shown or fetched`() = runTest {
        // Arrange
        coEvery { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) } returns
            Either.Left(IllegalStateException("Card session failed"))

        // Act
        createImplementor(scope = this).onGenerateMissedAddressesClick(userWalletId, currencies)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { deriveCurrencyAndOwnerKeysUseCase(userWalletId, currencies) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        coVerify(exactly = 0) { multiNetworkStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiQuoteStatusFetcher(params = any()) }
        coVerify(exactly = 0) { multiStakingBalanceFetcher(params = any()) }
    }

    @Test
    fun `GIVEN hot wallet banner WHEN upgrade click THEN the launcher opens the upgrade from Main`() = runTest {
        // Act
        createImplementor(scope = this).onUpgradeHotWalletClick(userWalletId)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(any<WalletScreenAnalyticsEvent.MainScreen.ButtonUpgrade>()) }
        coVerify(exactly = 1) {
            hardwareUpgradeLauncher.launch(userWalletId, AppRoute.HardwareUpgradeStories.Source.MAIN)
        }
    }

    @Test
    fun `GIVEN upgrade launch pending WHEN screen paused THEN the launch is cancelled`() = runTest {
        // Arrange
        val launchCancelled = CompletableDeferred<Boolean>()
        coEvery { hardwareUpgradeLauncher.launch(any(), any()) } coAnswers {
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                launchCancelled.complete(true)
                throw e
            }
        }
        createImplementor(scope = this).onUpgradeHotWalletClick(userWalletId)
        advanceUntilIdle()
        val isCancelledBeforePause = launchCancelled.isCompleted

        // Act
        screenLifecycleProvider.onPause()
        advanceUntilIdle()

        // Assert
        assertThat(isCancelledBeforePause).isFalse()
        assertThat(launchCancelled.isCompleted).isTrue()
    }

    private fun provideTestModels() = listOf(DerivationOutcome.DERIVED, DerivationOutcome.NOTHING_MISSING)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnLinkWalletClick {

        private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }

        private val walletLinkProgressHolder = WalletLinkProgressHolder()

        @Test
        fun `GIVEN link in progress WHEN link wallet click THEN wallet is linking and no dialog`() = runTest {
            // Arrange
            val pendingLink = CompletableDeferred<Unit>()
            coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers { pendingLink.await().right() }

            // Act
            createImplementor(scope = this, walletLinkProgressHolder).onLinkWalletClick(userWallet)
            advanceUntilIdle()

            // Assert
            assertThat(walletLinkProgressHolder.isLinking(userWalletId).first()).isTrue()
            verify(exactly = 0) { uiMessageSender.send(any()) }
            pendingLink.complete(Unit)
            advanceUntilIdle()
            assertThat(walletLinkProgressHolder.isLinking(userWalletId).first()).isFalse()
        }

        @Test
        fun `GIVEN link in progress WHEN link wallet clicked again THEN link is not started twice`() = runTest {
            // Arrange
            val pendingLink = CompletableDeferred<Unit>()
            coEvery { linkWalletToDeviceUseCase(userWallet) } coAnswers { pendingLink.await().right() }
            val implementor = createImplementor(scope = this, walletLinkProgressHolder)
            implementor.onLinkWalletClick(userWallet)
            advanceUntilIdle()

            // Act
            implementor.onLinkWalletClick(userWallet)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { linkWalletToDeviceUseCase(userWallet) }
            verify(exactly = 1) { analyticsEventHandler.send(WalletLinkAnalyticsEvent.ButtonLinkWallet()) }
            pendingLink.complete(Unit)
            advanceUntilIdle()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN link fails WHEN link wallet click THEN linking ends and error or cancel is reported`(
            model: LinkFailureModel,
        ) = runTest {
            // Arrange
            coEvery { linkWalletToDeviceUseCase(userWallet) } returns model.error.left()

            // Act
            createImplementor(scope = this, walletLinkProgressHolder).onLinkWalletClick(userWallet)
            advanceUntilIdle()

            // Assert
            assertThat(walletLinkProgressHolder.isLinking(userWalletId).first()).isFalse()
            verify(exactly = if (model.isErrorShown) 1 else 0) { uiMessageSender.send(any<DialogMessage>()) }
            verify(exactly = if (model.isCanceledReported) 1 else 0) {
                analyticsEventHandler.send(WalletLinkAnalyticsEvent.Canceled())
            }
        }

        private fun provideTestModels() = listOf(
            LinkFailureModel(error = WalletLinkError.Cancelled, isErrorShown = false, isCanceledReported = true),
            LinkFailureModel(error = WalletLinkError.WrongCard, isErrorShown = false, isCanceledReported = false),
            LinkFailureModel(
                error = WalletLinkError.Failed(reason = "api"),
                isErrorShown = true,
                isCanceledReported = false,
            ),
            LinkFailureModel(error = WalletLinkError.NotSupported, isErrorShown = true, isCanceledReported = false),
        )
    }

    data class LinkFailureModel(
        val error: WalletLinkError,
        val isErrorShown: Boolean,
        val isCanceledReported: Boolean,
    )

    private fun createImplementor(
        scope: TestScope,
        walletLinkProgressHolder: WalletLinkProgressHolder = WalletLinkProgressHolder(),
    ): WalletWarningsClickIntentsImplementor {
        val implementor = WalletWarningsClickIntentsImplementor(
            stateHolder = stateHolder,
            walletEventSender = mockk(relaxed = true),
            deriveCurrencyAndOwnerKeysUseCase = deriveCurrencyAndOwnerKeysUseCase,
            setCardWasScannedUseCase = mockk(relaxed = true),
            neverToSuggestRateAppUseCase = mockk(relaxed = true),
            remindToRateAppLaterUseCase = mockk(relaxed = true),
            getUserWalletUseCase = mockk(relaxed = true),
            nonBiometricUnlockWalletUseCase = mockk(relaxed = true),
            analyticsEventHandler = analyticsEventHandler,
            dispatchers = scope.createTestingCoroutineDispatcherProvider(),
            getWalletMetaInfoUseCase = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            appStoreOpener = mockk(relaxed = true),
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            navigationRouter = mockk(relaxed = true),
            userWalletsListRepository = mockk(relaxed = true),
            setShouldShowNotificationUseCase = mockk(relaxed = true),
            notificationsRepository = mockk(relaxed = true),
            uiMessageSender = uiMessageSender,
            reviewManager = mockk(relaxed = true),
            closeHotWalletUpgradeBannerUseCase = mockk(relaxed = true),
            acknowledgeAssetsDiscoveryCompletionUseCase = mockk(relaxed = true),
            yieldSupplySetShouldShowMainPromoUseCase = mockk(relaxed = true),
            hardwareUpgradeLauncher = hardwareUpgradeLauncher,
            screenLifecycleProvider = screenLifecycleProvider,
            collectiblesAccountStatusFetcher = collectiblesAccountStatusFetcher,
            collectiblesFeatureToggles = collectiblesFeatureToggles,
            linkWalletToDeviceUseCase = linkWalletToDeviceUseCase,
            walletLinkProgressHolder = walletLinkProgressHolder,
        )
        implementor.initialize(router = mockk(relaxed = true), coroutineScope = scope)

        return implementor
    }
}