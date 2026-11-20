package com.tangem.common.ui.notifications

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.NetworkFeeUnavailableAnalyticsEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class NetworkFeeUnavailableNoticeAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val source = NetworkFeeUnavailableAnalyticsEvent.Source.Send
    private val expectedEvent = NetworkFeeUnavailableAnalyticsEvent(source)
    private val feeUnreachable = NotificationUM.Warning.NetworkFeeUnreachable(onRefresh = {})

    private lateinit var noticeAnalytics: NetworkFeeUnavailableNoticeAnalytics

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsEventHandler)
        noticeAnalytics = NetworkFeeUnavailableNoticeAnalytics(analyticsEventHandler, source)
    }

    @Test
    fun `GIVEN fee unreachable banner WHEN notifications updated THEN event sent`() {
        // Act
        noticeAnalytics.onNotificationsUpdated(listOf(feeUnreachable))

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(expectedEvent) }
    }

    @Test
    fun `GIVEN no fee unreachable banner WHEN notifications updated THEN event not sent`() {
        // Act
        noticeAnalytics.onNotificationsUpdated(listOf(NotificationUM.Error.TotalExceedsBalance))

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN banner still shown WHEN notifications rebuilt THEN event sent once`() {
        // Act
        noticeAnalytics.onNotificationsUpdated(listOf(feeUnreachable))
        noticeAnalytics.onNotificationsUpdated(listOf(feeUnreachable, NotificationUM.Error.TotalExceedsBalance))

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(expectedEvent) }
    }

    @Test
    fun `GIVEN banner hidden in between WHEN banner shown again THEN event sent again`() {
        // Act
        noticeAnalytics.onBannerVisibilityChanged(isShown = true)
        noticeAnalytics.onBannerVisibilityChanged(isShown = false)
        noticeAnalytics.onBannerVisibilityChanged(isShown = true)

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(expectedEvent) }
    }
}