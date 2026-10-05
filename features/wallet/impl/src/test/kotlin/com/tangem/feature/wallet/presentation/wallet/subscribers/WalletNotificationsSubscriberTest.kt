package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.right
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsCarouselFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.features.hotwallet.HardwareUpgradeLauncher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletNotificationsSubscriberTest {

    private val stateHolder: WalletStateController = mockk(relaxed = true)
    private val clickIntents: WalletClickIntents = mockk(relaxed = true)
    private val getWalletNotificationsFactory: GetWalletNotificationsFactory = mockk()
    private val getWalletNotificationsCarouselFactory: GetWalletNotificationsCarouselFactory = mockk()
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender = mockk(relaxed = true)
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender = mockk(relaxed = true)
    private val getStoryContentUseCase: GetStoryContentUseCase = mockk()
    private val hardwareUpgradeLauncher: HardwareUpgradeLauncher = mockk(relaxUnitFun = true)
    private val userWallet: UserWallet = mockk(relaxed = true)

    private val subscriber = WalletNotificationsSubscriber(
        userWallet = userWallet,
        stateHolder = stateHolder,
        clickIntents = clickIntents,
        getWalletNotificationsFactory = getWalletNotificationsFactory,
        getWalletNotificationsCarouselFactory = getWalletNotificationsCarouselFactory,
        walletWarningsAnalyticsSender = walletWarningsAnalyticsSender,
        walletWarningsSingleEventSender = walletWarningsSingleEventSender,
        getStoryContentUseCase = getStoryContentUseCase,
        hardwareUpgradeLauncher = hardwareUpgradeLauncher,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            stateHolder,
            getWalletNotificationsFactory,
            getWalletNotificationsCarouselFactory,
            getStoryContentUseCase,
            hardwareUpgradeLauncher,
            userWallet,
        )
        every { userWallet.walletId } returns WALLET_ID

        val walletUM = mockk<WalletUM>(relaxed = true)
        every { walletUM.walletsBalanceUM.id } returns WALLET_ID
        val screenState = mockk<WalletScreenState>(relaxed = true)
        every { screenState.wallets2 } returns persistentListOf(walletUM)
        every { stateHolder.uiState } returns MutableStateFlow(screenState)
        every { stateHolder.getWalletUM(any()) } returns walletUM

        every { getWalletNotificationsFactory.create(any(), any()) } returns flowOf(persistentListOf())
        coEvery { getStoryContentUseCase.invokeSync(any(), any()) } returns null.right()
    }

    @Test
    fun `GIVEN carousel has yield boost banner WHEN subscribed THEN yield story is prefetched`() = runTest {
        // Arrange
        every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(yieldBoostCarousel())

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
        every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(persistentListOf())

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
            val notifications = MutableStateFlow(upgradeNotifications())
            every { getWalletNotificationsFactory.create(any(), any()) } returns notifications
            every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(persistentListOf())
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            subscriber.subscribe(scope, dispatcher)
            advanceUntilIdle()

            // Act
            notifications.value = upgradeNotifications()
            advanceUntilIdle()
            notifications.value = persistentListOf()
            advanceUntilIdle()
            notifications.value = upgradeNotifications()
            advanceUntilIdle()
            scope.cancel()

            // Assert
            verify(exactly = 2) { hardwareUpgradeLauncher.prefetch() }
        }

    @Test
    fun `GIVEN no upgrade banner WHEN subscribed THEN upgrade story prefetch is not requested`() = runTest {
        // Arrange
        every { getWalletNotificationsCarouselFactory.create(any(), any()) } returns flowOf(persistentListOf())

        // Act
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)
        subscriber.subscribe(scope, dispatcher)
        advanceUntilIdle()
        scope.cancel()

        // Assert
        verify(exactly = 0) { hardwareUpgradeLauncher.prefetch() }
    }

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
    }
}