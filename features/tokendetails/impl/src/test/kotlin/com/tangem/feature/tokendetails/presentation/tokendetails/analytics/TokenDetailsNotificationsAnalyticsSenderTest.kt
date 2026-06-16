package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test

internal class TokenDetailsNotificationsAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val network: Network = mockk(relaxed = true) {
        every { name } returns "Ethereum"
    }
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { symbol } returns "ETH"
        every { this@mockk.network } returns this@TokenDetailsNotificationsAnalyticsSenderTest.network
    }

    private val sender = TokenDetailsNotificationsAnalyticsSender(
        cryptoCurrency = cryptoCurrency,
        analyticsEventHandler = analyticsEventHandler,
    )

    @Test
    fun `GIVEN NetworkFee notification WHEN send THEN NotEnoughFee event with DetailedScreen source is sent`() {
        // GIVEN
        val notification = mockk<TokenDetailsNotification.NetworkFee>()
        val displayedState = createState(notifications = emptyList(), isRefreshing = false)
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        // WHEN
        sender.send(displayedUiState = displayedState, newNotifications = listOf(notification))

        // THEN
        val event = eventSlot.captured as TokenDetailsAnalyticsEvent.Notice.NotEnoughFee
        assertThat(event.category).isEqualTo("Token")
        assertThat(event.event).isEqualTo("Notice - Not Enough Fee")
        assertThat(event.params).containsEntry("Token", "ETH")
        assertThat(event.params).containsEntry("Blockchain", "Ethereum")
        assertThat(event.params).containsEntry("Source", "Detailed Screen")
    }

    @Test
    fun `GIVEN NetworkFeeWithBuyButton notification WHEN send THEN NotEnoughFee event with DetailedScreen source is sent`() {
        // GIVEN
        val notification = mockk<TokenDetailsNotification.NetworkFeeWithBuyButton>()
        val displayedState = createState(notifications = emptyList(), isRefreshing = false)
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        // WHEN
        sender.send(displayedUiState = displayedState, newNotifications = listOf(notification))

        // THEN
        val event = eventSlot.captured as TokenDetailsAnalyticsEvent.Notice.NotEnoughFee
        assertThat(event.params).containsEntry("Source", "Detailed Screen")
    }

    @Test
    fun `GIVEN DynamicAddressesFundsFound notification WHEN send THEN AdditionalAddressesFound event is sent`() {
        // GIVEN
        val notification = TokenDetailsNotification.DynamicAddressesFundsFound(onLearnMoreClick = {})
        val displayedState = createState(notifications = emptyList(), isRefreshing = false)
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        // WHEN
        sender.send(displayedUiState = displayedState, newNotifications = listOf(notification))

        // THEN
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured as TokenDetailsAnalyticsEvent.Notice.AdditionalAddressesFound
        assertThat(event.category).isEqualTo("Token")
        assertThat(event.event).isEqualTo("Notice - Additional Addresses Found")
        assertThat(event.params).containsEntry("Token", "ETH")
        assertThat(event.params).containsEntry("Blockchain", "Ethereum")
    }

    @Test
    fun `GIVEN empty new notifications WHEN send THEN no event is sent`() {
        // GIVEN
        val displayedState = createState(notifications = emptyList(), isRefreshing = false)

        // WHEN
        sender.send(displayedUiState = displayedState, newNotifications = emptyList())

        // THEN
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN pullToRefresh is refreshing WHEN send THEN no event is sent`() {
        // GIVEN
        val notification = TokenDetailsNotification.DynamicAddressesFundsFound(onLearnMoreClick = {})
        val displayedState = createState(notifications = emptyList(), isRefreshing = true)

        // WHEN
        sender.send(displayedUiState = displayedState, newNotifications = listOf(notification))

        // THEN
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN notification without matching event WHEN send THEN no event is sent`() {
        // GIVEN: NetworksUnreachable is an unmapped notification (returns null in getEvent)
        val notification = TokenDetailsNotification.NetworksUnreachable
        val displayedState = createState(notifications = emptyList(), isRefreshing = false)

        // WHEN
        sender.send(displayedUiState = displayedState, newNotifications = listOf(notification))

        // THEN
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    private fun createState(
        notifications: List<TokenDetailsNotification>,
        isRefreshing: Boolean,
    ): TokenDetailsState {
        return mockk(relaxed = true) {
            every { this@mockk.notifications } returns notifications.toPersistentList()
            every { pullToRefreshConfig.isRefreshing } returns isRefreshing
        }
    }
}