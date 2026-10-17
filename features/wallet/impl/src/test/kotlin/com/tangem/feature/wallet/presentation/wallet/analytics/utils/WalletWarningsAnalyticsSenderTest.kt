package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WalletWarningsAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val screenLifecycleProvider = ScreenLifecycleProvider()

    private val sender = WalletWarningsAnalyticsSender(
        analyticsEventHandler = analyticsEventHandler,
        screenLifecycleProvider = screenLifecycleProvider,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
        // Like the context interceptors, the handler enriches the sent instance in place
        every { analyticsEventHandler.send(any()) } answers {
            firstArg<AnalyticsEvent>().withParams(mapOf("Product Type" to "Mobile Wallet"))
        }
    }

    @Test
    fun `GIVEN upgrade notice sent WHEN the banner is emitted again THEN the notice is not sent again`() {
        // Arrange
        send(notifications = persistentListOf(createUpgradePromo()))

        // Act
        send(notifications = persistentListOf(createUpgradePromo()))

        // Assert
        verifyUpgradeNoticeSent(times = 1)
    }

    @Test
    fun `GIVEN banner appeared while the screen was paused WHEN emitted after resume THEN the notice is sent`() {
        // Arrange
        screenLifecycleProvider.onPause()
        send(notifications = persistentListOf(createUpgradePromo()))
        screenLifecycleProvider.onResume()

        // Act
        send(notifications = persistentListOf(createUpgradePromo()))

        // Assert
        verifyUpgradeNoticeSent(times = 1)
    }

    @Test
    fun `GIVEN upgrade notice sent WHEN the banner disappears and comes back THEN the notice is sent again`() {
        // Arrange
        send(notifications = persistentListOf(createUpgradePromo()))
        send(notifications = persistentListOf())

        // Act
        send(notifications = persistentListOf(createUpgradePromo()))

        // Assert
        verifyUpgradeNoticeSent(times = 2)
    }

    @Test
    fun `GIVEN upgrade notice sent for one wallet WHEN another wallet shows the banner THEN its notice is sent`() {
        // Arrange
        send(notifications = persistentListOf(createUpgradePromo()))

        // Act
        send(notifications = persistentListOf(createUpgradePromo()), userWalletId = OTHER_WALLET_ID)

        // Assert
        verifyUpgradeNoticeSent(times = 2)
    }

    private fun send(notifications: ImmutableList<WalletNotificationUM>, userWalletId: UserWalletId = WALLET_ID) {
        sender.send(
            userWalletId = userWalletId,
            displayedWalletUM = createWalletUM(),
            newNotifications = notifications,
        )
    }

    private fun verifyUpgradeNoticeSent(times: Int) {
        verify(exactly = times) {
            analyticsEventHandler.send(any<WalletScreenAnalyticsEvent.MainScreen.NoticeUpgradeToColdWallet>())
        }
    }

    /** Capturing lambdas, as in production: each copy of the banner is a new object that never equals the last one. */
    private fun createUpgradePromo(): WalletNotificationUM.UpgradeHotWalletPromo {
        val clickTarget = Any()
        return WalletNotificationUM.UpgradeHotWalletPromo(
            onLaterClick = { clickTarget.hashCode() },
            onUpgradeClick = { clickTarget.hashCode() },
        )
    }

    private fun createWalletUM(): WalletUM = mockk {
        every { pullToRefreshConfig } returns PullToRefreshConfig(isRefreshing = false, onRefresh = {})
    }

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val OTHER_WALLET_ID = UserWalletId("02")
    }
}