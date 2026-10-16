package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletIcon
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetPendingJointOperationsFlowFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsCarouselFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.NotificationsBalanceState
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNotificationsSnapshot
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.JointOperationBannerUM
import com.tangem.feature.wallet.presentation.wallet.state.model.PromoCardsCheck
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.state.model.createWalletContentUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.ReinitializeWalletTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetJointOperationBannersTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetRefreshStateTransformer
import com.tangem.features.hotwallet.HardwareUpgradeLauncher
import com.tangem.features.promobanners.api.PromoBannersFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletNotificationsSubscriberTest {

    private val stateHolder: WalletStateController = mockk(relaxed = true)
    private val clickIntents: WalletClickIntents = mockk(relaxed = true)
    private val getWalletNotificationsFactory: GetWalletNotificationsFactory = mockk()
    private val getWalletNotificationsCarouselFactory: GetWalletNotificationsCarouselFactory = mockk()
    private val getPendingJointOperationsFlowFactory: GetPendingJointOperationsFlowFactory = mockk()
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender = mockk(relaxed = true)
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender = mockk(relaxed = true)
    private val getStoryContentUseCase: GetStoryContentUseCase = mockk()
    private val hardwareUpgradeLauncher: HardwareUpgradeLauncher = mockk(relaxUnitFun = true)
    private val promoBannersFeatureToggles: PromoBannersFeatureToggles = mockk()
    private val userWallet: UserWallet = mockk(relaxed = true)
    private val reinitializedWallet: UserWallet.Hot = mockk(relaxed = true)
    private val walletImageResolver: WalletImageResolver = mockk()
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk()

    private val subscriber = WalletNotificationsSubscriber(
        userWallet = userWallet,
        stateHolder = stateHolder,
        clickIntents = clickIntents,
        getWalletNotificationsFactory = getWalletNotificationsFactory,
        getWalletNotificationsCarouselFactory = getWalletNotificationsCarouselFactory,
        getPendingJointOperationsFlowFactory = getPendingJointOperationsFlowFactory,
        walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
        walletWarningsSingleEventSender = walletWarningsSingleEventSender,
        getStoryContentUseCase = getStoryContentUseCase,
        hardwareUpgradeLauncher = hardwareUpgradeLauncher,
        promoBannersFeatureToggles = promoBannersFeatureToggles,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            stateHolder,
            getWalletNotificationsFactory,
            getWalletNotificationsCarouselFactory,
            getPendingJointOperationsFlowFactory,
            getStoryContentUseCase,
            hardwareUpgradeLauncher,
            promoBannersFeatureToggles,
            walletWarningsAnalyticsSender,
            userWallet,
            reinitializedWallet,
            getWalletIconUseCase,
        )
        every { userWallet.walletId } returns WALLET_ID
        every { promoBannersFeatureToggles.isHideMarketingBannersWhileSystemEnabled } returns false

        val walletUM = mockk<WalletUM>(relaxed = true)
        every { walletUM.walletsBalanceUM.id } returns WALLET_ID
        val screenState = mockk<WalletScreenState>(relaxed = true)
        every { screenState.wallets2 } returns persistentListOf(walletUM)
        every { stateHolder.uiState } returns MutableStateFlow(screenState)
        every { stateHolder.getWalletUM(any()) } returns walletUM

        every { getWalletNotificationsFactory.create(any(), any()) } returns flowOf(snapshot())
        every { getPendingJointOperationsFlowFactory.create(any()) } returns flowOf(emptyList())
        coEvery { getStoryContentUseCase.invokeSync(any(), any()) } returns null.right()
    }

    @Test
    fun `GIVEN carousel has yield boost banner WHEN subscribed THEN yield story is prefetched`() = runTest {
        // Arrange
        every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns
            flowOf(snapshot(yieldBoostCarousel()))

        // Act
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        subscriber.subscribe(scope, dispatcher)
        advanceUntilIdle()
        scope.cancel()

        // Assert
        coVerify(exactly = 1) {
            getStoryContentUseCase.invokeSync(id = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id, refresh = true)
        }
    }

    @Test
    fun `GIVEN carousel has no yield boost banner WHEN subscribed THEN yield story is not prefetched`() = runTest {
        // Arrange
        every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(snapshot())

        // Act
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        subscriber.subscribe(scope, dispatcher)
        advanceUntilIdle()
        scope.cancel()

        // Assert
        coVerify(exactly = 0) {
            getStoryContentUseCase.invokeSync(id = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id, refresh = true)
        }
    }

    @Test
    fun `GIVEN upgrade banner stays then reappears WHEN notifications re-emit THEN prefetch once per appearance`() =
        runTest {
            // Arrange
            val notifications = MutableStateFlow(snapshot(upgradeNotifications()))
            every { getWalletNotificationsFactory.create(any(), any()) } returns notifications
            every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(snapshot())
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            subscriber.subscribe(scope, dispatcher)
            advanceUntilIdle()

            // Act
            notifications.value = snapshot(upgradeNotifications())
            advanceUntilIdle()
            notifications.value = snapshot()
            advanceUntilIdle()
            notifications.value = snapshot(upgradeNotifications())
            advanceUntilIdle()
            scope.cancel()

            // Assert
            verify(exactly = 2) { hardwareUpgradeLauncher.prefetch() }
        }

    @Test
    fun `GIVEN no upgrade banner WHEN subscribed THEN upgrade story prefetch is not requested`() = runTest {
        // Arrange
        every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(snapshot())

        // Act
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        subscriber.subscribe(scope, dispatcher)
        advanceUntilIdle()
        scope.cancel()

        // Assert
        verify(exactly = 0) { hardwareUpgradeLauncher.prefetch() }
    }

    @Nested
    inner class PromoCards {

        private val stateController = WalletStateController()
        private val notifications = MutableStateFlow(snapshot())
        private val notificationsCarousel = MutableStateFlow(snapshot())
        private val sentNotifications = mutableListOf<List<WalletNotificationUM>>()
        private val jointOperations = MutableStateFlow<List<JointPendingOperation>>(emptyList())

        private val subscriber = WalletNotificationsSubscriber(
            userWallet = userWallet,
            stateHolder = stateController,
            clickIntents = clickIntents,
            getWalletNotificationsFactory = getWalletNotificationsFactory,
            getWalletNotificationsCarouselFactory = getWalletNotificationsCarouselFactory,
            getPendingJointOperationsFlowFactory = getPendingJointOperationsFlowFactory,
            walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
            walletWarningsSingleEventSender = walletWarningsSingleEventSender,
            getStoryContentUseCase = getStoryContentUseCase,
            hardwareUpgradeLauncher = hardwareUpgradeLauncher,
            promoBannersFeatureToggles = promoBannersFeatureToggles,
        )

        @BeforeEach
        fun setupPromoCards() {
            every { promoBannersFeatureToggles.isHideMarketingBannersWhileSystemEnabled } returns true
            every { getWalletNotificationsFactory.create(any(), any()) } returns notifications
            every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns notificationsCarousel
            every { getPendingJointOperationsFlowFactory.create(any()) } returns jointOperations
            every { walletWarningsAnalyticsSender.send(any(), any(), capture(sentNotifications)) } just Runs
            stateController.update { it.copy(wallets2 = persistentListOf(createWalletContentUM(WALLET_ID))) }
        }

        @Test
        fun `GIVEN hiding turned off WHEN notifications are emitted THEN they are set as they are`() = runTest {
            // Arrange
            every { promoBannersFeatureToggles.isHideMarketingBannersWhileSystemEnabled } returns false
            notifications.value = snapshot(
                missingBackup,
                tangemPayPromo,
                balanceState = NotificationsBalanceState.Loading,
            )

            // Act
            subscribe()

            // Assert
            val expected = createWalletContentUM(
                userWalletId = WALLET_ID,
                notifications = persistentListOf(missingBackup, tangemPayPromo),
            )
            assertThat(stateController.getWalletUM(WALLET_ID)).isEqualTo(expected)
            assertThat(sentNotifications).containsExactly(listOf(missingBackup, tangemPayPromo))
            verify(exactly = 0) { getPendingJointOperationsFlowFactory.create(any()) }
        }

        @Test
        fun `GIVEN hiding turned off WHEN only balance, joint banners or refresh change THEN nothing is set again`() =
            runTest {
                // Arrange
                every { promoBannersFeatureToggles.isHideMarketingBannersWhileSystemEnabled } returns false
                notifications.value = snapshot(
                    missingBackup,
                    tangemPayPromo,
                    balanceState = NotificationsBalanceState.Loading,
                )
                subscribe()

                // Act
                notifications.value = snapshot(missingBackup, tangemPayPromo)
                notificationsCarousel.value = snapshot(balanceState = NotificationsBalanceState.Loading)
                stateController.update(SetJointOperationBannersTransformer(WALLET_ID, persistentListOf(jointBanner)))
                stateController.update(SetRefreshStateTransformer(WALLET_ID, isRefreshing = true))
                stateController.update(SetRefreshStateTransformer(WALLET_ID, isRefreshing = false))
                advanceUntilIdle()

                // Assert
                assertThat(sentNotifications).containsExactly(listOf(missingBackup, tangemPayPromo))
            }

        @Test
        fun `GIVEN no system card WHEN the balance resolves THEN Tangem Pay promo is shown and reported`() = runTest {
            // Arrange
            notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Loading)
            subscribe()

            // Act
            notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Resolved)
            advanceUntilIdle()

            // Assert
            val expected = createWalletContentUM(
                userWalletId = WALLET_ID,
                notifications = persistentListOf(tangemPayPromo),
                isMarketingBlockVisible = true,
                isTangemPayPromoVisible = true,
                promoCardsCheck = PromoCardsCheck.None,
            )
            assertThat(stateController.getWalletUM(WALLET_ID)).isEqualTo(expected)
            // The sender skips a list equal to the last one it got, so only the changes reach analytics
            val sentChanges = sentNotifications.filterIndexed { index, sent ->
                index == 0 || sent != sentNotifications[index - 1]
            }
            assertThat(sentChanges)
                .containsExactly(emptyList<WalletNotificationUM>(), listOf(tangemPayPromo))
                .inOrder()
        }

        @Test
        fun `GIVEN the last system card gone WHEN a refresh finishes THEN promo cards wait for the settled balance`() =
            runTest {
                // Arrange
                notifications.value = snapshot(missingBackup, tangemPayPromo)
                subscribe()
                notifications.value = snapshot(tangemPayPromo)
                advanceUntilIdle()
                val isShownWithoutCheck = isPromoCardsShown()

                // Act
                stateController.update(SetRefreshStateTransformer(WALLET_ID, isRefreshing = true))
                notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Resolved)
                stateController.update(SetRefreshStateTransformer(WALLET_ID, isRefreshing = false))
                advanceUntilIdle()
                val isShownBeforeSettled = isPromoCardsShown()
                notifications.value = snapshot(tangemPayPromo)
                advanceUntilIdle()
                val isShownWhenSettled = isPromoCardsShown()

                // Assert
                assertThat(listOf(isShownWithoutCheck, isShownBeforeSettled, isShownWhenSettled))
                    .containsExactly(false, false, true)
                    .inOrder()
            }

        @Test
        fun `GIVEN shown promo cards WHEN joint operations come and go THEN they hide until a check`() =
            runTest {
                // Arrange
                notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Resolved)
                subscribe()
                val isShownBeforeOperations = isPromoCardsShown()

                // Act
                jointOperations.value = listOf(jointOperation)
                advanceUntilIdle()
                val isShownWithOperations = isPromoCardsShown()
                jointOperations.value = emptyList()
                advanceUntilIdle()

                // Assert
                assertThat(listOf(isShownBeforeOperations, isShownWithOperations))
                    .containsExactly(true, false)
                    .inOrder()
                val expected = createWalletContentUM(
                    userWalletId = WALLET_ID,
                    promoCardsCheck = PromoCardsCheck.None,
                )
                assertThat(stateController.getWalletUM(WALLET_ID)).isEqualTo(expected)
            }

        @Test
        fun `GIVEN promo cards hidden by an upgrade card WHEN the wallet is reinitialized THEN they are rechecked`() =
            runTest {
                // Arrange
                every { reinitializedWallet.walletId } returns WALLET_ID
                every { getWalletIconUseCase(reinitializedWallet) } returns UserWalletIcon.Hot
                notifications.value = snapshot(upgradeHotWalletPromo, tangemPayPromo)
                subscribe()
                notifications.value = snapshot(tangemPayPromo)
                advanceUntilIdle()
                val isShownBeforeReinitialization = isPromoCardsShown()

                // Act
                stateController.update(
                    ReinitializeWalletTransformer(
                        userWallet = reinitializedWallet,
                        clickIntents = clickIntents,
                        walletImageResolver = walletImageResolver,
                        getWalletIconUseCase = getWalletIconUseCase,
                    ),
                )
                advanceUntilIdle()

                // Assert
                assertThat(listOf(isShownBeforeReinitialization, isPromoCardsShown()))
                    .containsExactly(false, true)
                    .inOrder()
            }

        @Test
        fun `GIVEN the carousel still loading WHEN the notifications resolve THEN the open check waits for it`() =
            runTest {
                // Arrange
                notificationsCarousel.value = snapshot(balanceState = NotificationsBalanceState.Loading)
                notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Resolved)
                subscribe()
                val isShownWhileCarouselLoads = isPromoCardsShown()

                // Act
                notificationsCarousel.value = snapshot(balanceState = NotificationsBalanceState.Settled)
                advanceUntilIdle()

                // Assert
                assertThat(listOf(isShownWhileCarouselLoads, isPromoCardsShown()))
                    .containsExactly(false, true)
                    .inOrder()
            }

        @Test
        fun `GIVEN only Get your first crypto WHEN the balance resolves THEN Tangem Pay promo is shown above it`() =
            runTest {
                // Arrange
                notifications.value = snapshot(
                    addFunds,
                    tangemPayPromo,
                    balanceState = NotificationsBalanceState.Loading,
                )
                subscribe()

                // Act
                notifications.value = snapshot(
                    addFunds,
                    tangemPayPromo,
                    balanceState = NotificationsBalanceState.Resolved,
                )
                advanceUntilIdle()

                // Assert
                val expected = createWalletContentUM(
                    userWalletId = WALLET_ID,
                    notifications = persistentListOf(tangemPayPromo, addFunds),
                    isTangemPayPromoVisible = true,
                    promoCardsCheck = PromoCardsCheck.None,
                )
                assertThat(stateController.getWalletUM(WALLET_ID)).isEqualTo(expected)
                val sentChanges = sentNotifications.filterIndexed { index, sent ->
                    index == 0 || sent != sentNotifications[index - 1]
                }
                assertThat(sentChanges)
                    .containsExactly(listOf(addFunds), listOf(tangemPayPromo, addFunds))
                    .inOrder()
            }

        @Test
        fun `GIVEN Tangem Pay promo with Get your first crypto WHEN a top-up brings Finalize setup THEN it hides`() =
            runTest {
                // Arrange
                notifications.value = snapshot(
                    addFunds,
                    tangemPayPromo,
                    balanceState = NotificationsBalanceState.Resolved,
                )
                subscribe()
                val notificationsBeforeTopUp = stateController.getWalletUM(WALLET_ID)?.notifications

                // Act
                notifications.value = snapshot(finishWalletActivation, tangemPayPromo)
                advanceUntilIdle()

                // Assert
                assertThat(listOf(notificationsBeforeTopUp, stateController.getWalletUM(WALLET_ID)?.notifications))
                    .containsExactly(listOf(tangemPayPromo, addFunds), listOf(finishWalletActivation))
                    .inOrder()
            }

        @Test
        fun `GIVEN joint operations known later WHEN the balance resolves from cache THEN promo cards stay hidden`() =
            runTest {
                // Arrange
                val lateJointOperations = MutableSharedFlow<List<JointPendingOperation>>(replay = 1)
                every { getPendingJointOperationsFlowFactory.create(any()) } returns lateJointOperations
                notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Resolved)
                subscribe()
                val isShownBeforeOperations = isPromoCardsShown()

                // Act
                lateJointOperations.emit(listOf(jointOperation))
                advanceUntilIdle()
                val isShownWithOperations = isPromoCardsShown()
                lateJointOperations.emit(emptyList())
                advanceUntilIdle()

                // Assert
                assertThat(listOf(isShownBeforeOperations, isShownWithOperations, isPromoCardsShown()))
                    .containsExactly(false, false, false)
                    .inOrder()
                val expected = createWalletContentUM(userWalletId = WALLET_ID, promoCardsCheck = PromoCardsCheck.None)
                assertThat(stateController.getWalletUM(WALLET_ID)).isEqualTo(expected)
                assertThat(sentNotifications.flatten()).doesNotContain(tangemPayPromo)
            }

        @Test
        fun `GIVEN joint operations not known yet WHEN notifications come THEN they are set without waiting`() =
            runTest {
                // Arrange
                every { getPendingJointOperationsFlowFactory.create(any()) } returns MutableSharedFlow()
                notifications.value = snapshot(
                    missingBackup,
                    tangemPayPromo,
                    balanceState = NotificationsBalanceState.Resolved,
                )

                // Act
                subscribe()

                // Assert
                val expected = createWalletContentUM(
                    userWalletId = WALLET_ID,
                    notifications = persistentListOf(missingBackup),
                    promoCardsCheck = PromoCardsCheck.OnOpen,
                )
                assertThat(stateController.getWalletUM(WALLET_ID)).isEqualTo(expected)
            }

        @Test
        fun `GIVEN joint operations failing WHEN the balance resolves THEN the open check goes without them`() =
            runTest {
                // Arrange
                every { getPendingJointOperationsFlowFactory.create(any()) } returns flow {
                    throw IllegalStateException("Joint accounts are unavailable")
                }
                notifications.value = snapshot(tangemPayPromo, balanceState = NotificationsBalanceState.Resolved)

                // Act
                subscribe()

                // Assert
                assertThat(isPromoCardsShown()).isTrue()
            }

        /** Collects in [TestScope.backgroundScope], so a failing test cannot leave the subscriber running */
        private fun TestScope.subscribe() {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            subscriber.subscribe(CoroutineScope(backgroundScope.coroutineContext + dispatcher), dispatcher)
            advanceUntilIdle()
        }

        /** Both promo cards: no card in these scenarios hides only one of them */
        private fun isPromoCardsShown(): Boolean {
            val walletUM = stateController.getWalletUM(WALLET_ID) as? WalletUM.Content ?: return false

            return walletUM.isMarketingBlockVisible && walletUM.isTangemPayPromoVisible
        }
    }

    private fun snapshot(
        vararg notifications: WalletNotificationUM,
        balanceState: NotificationsBalanceState = NotificationsBalanceState.Settled,
    ) = snapshot(persistentListOf(*notifications), balanceState)

    private fun snapshot(
        notifications: ImmutableList<WalletNotificationUM>,
        balanceState: NotificationsBalanceState = NotificationsBalanceState.Settled,
    ) = WalletNotificationsSnapshot(notifications = notifications, balanceState = balanceState)

    /** Capturing lambdas, as in production: each list is a new value, so the flow re-emits it. */
    private fun upgradeNotifications(): ImmutableList<WalletNotificationUM> {
        val clickTarget = Any()
        return persistentListOf(
            WalletNotificationUM.UpgradeHotWalletPromo(
                onLaterClick = { clickTarget.hashCode() },
                onUpgradeClick = { clickTarget.hashCode() },
            ),
        )
    }

    private fun yieldBoostCarousel(): ImmutableList<WalletNotificationUM> = persistentListOf(
        WalletNotificationUM.YieldBoostPromo(onExploreClick = {}, onLaterClick = {}),
    )

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val tangemPayPromo = WalletNotificationUM.TangemPayPromo(onLaterClick = {}, onLearnMoreClick = {})
        val missingBackup = WalletNotificationUM.MissingBackup(onClick = {})
        val addFunds = WalletNotificationUM.AddFunds(onClick = {})
        val finishWalletActivation = WalletNotificationUM.FinishWalletActivation(
            messageEffect = TangemMessageEffect.None,
            isBackupExists = false,
            onClick = {},
        )
        val upgradeHotWalletPromo = WalletNotificationUM.UpgradeHotWalletPromo(onLaterClick = {}, onUpgradeClick = {})
        val jointBanner = JointOperationBannerUM(operationId = "operation", banner = mockk())
        val jointOperation: JointPendingOperation = mockk()
    }
}