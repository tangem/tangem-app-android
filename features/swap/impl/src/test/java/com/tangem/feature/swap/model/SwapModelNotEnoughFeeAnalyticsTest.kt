package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for [SwapModel.updateNotEnoughFeeNotificationState].
 *
 * Quotes are reloaded on a timer, so the same [SwapNotificationUM.Error.UnableToCoverFeeWarning] is rebuilt
 * on every poll. The event must be reported once per appearance instead — the guard re-arms when the
 * notification clears or when the user switches to another pair.
 */
internal class SwapModelNotEnoughFeeAnalyticsTest : SwapModelTestBase() {

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val fromCurrency = currency(symbol = "USDT", networkName = "Ethereum", id = "tether")
    private val toCurrency = currency(symbol = "BTC", networkName = "Bitcoin", id = "bitcoin")
    private val unableToCoverFee: NotificationUM = mockk<SwapNotificationUM.Error.UnableToCoverFeeWarning>()

    @BeforeEach
    fun setUp() {
        setUpBase()
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
    }

    @Test
    fun `GIVEN unable to cover fee notification shown WHEN state handled THEN not enough fee event sent`() {
        // Arrange
        val model = modelShowing(unableToCoverFee)

        // Act
        model.updateNotEnoughFeeNotificationState(loadedState(toCurrency))

        // Assert
        assertThat(notEnoughFeeEvents()).containsExactly(expectedEvent)
    }

    @Test
    fun `GIVEN no unable to cover fee notification WHEN state handled THEN no event sent`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Warning.HighPriceImpact)

        // Act
        model.updateNotEnoughFeeNotificationState(loadedState(toCurrency))

        // Assert
        assertThat(notEnoughFeeEvents()).isEmpty()
    }

    @Test
    fun `GIVEN not enough fee reported WHEN quotes polled again THEN event not repeated`() {
        // Arrange
        val model = modelShowing(unableToCoverFee)
        val state = loadedState(toCurrency)
        model.updateNotEnoughFeeNotificationState(state)

        // Act
        model.updateNotEnoughFeeNotificationState(state)

        // Assert
        assertThat(notEnoughFeeEvents()).containsExactly(expectedEvent)
    }

    @Test
    fun `GIVEN notification cleared WHEN it appears again THEN event sent again`() {
        // Arrange
        val model = modelShowing(unableToCoverFee)
        val state = loadedState(toCurrency)
        model.updateNotEnoughFeeNotificationState(state)

        // Act
        model.uiState = model.uiState.copy(notifications = persistentListOf())
        model.updateNotEnoughFeeNotificationState(state)
        model.uiState = model.uiState.copy(notifications = persistentListOf(unableToCoverFee))
        model.updateNotEnoughFeeNotificationState(state)

        // Assert
        assertThat(notEnoughFeeEvents()).containsExactly(expectedEvent, expectedEvent)
    }

    @Test
    fun `GIVEN not enough fee reported WHEN quotes fail and it appears again THEN event sent again`() {
        // Arrange
        val model = modelShowing(unableToCoverFee)
        val state = loadedState(toCurrency)
        model.updateNotEnoughFeeNotificationState(state)

        // Act
        model.handleQuotesLoadingError(IllegalStateException("quotes are unavailable"))
        model.uiState = model.uiState.copy(notifications = persistentListOf(unableToCoverFee))
        model.updateNotEnoughFeeNotificationState(state)

        // Assert
        assertThat(notEnoughFeeEvents()).containsExactly(expectedEvent, expectedEvent)
    }

    @Test
    fun `GIVEN not enough fee reported WHEN amount cleared and re-entered THEN event sent again`() {
        // Arrange
        val model = modelShowing(unableToCoverFee)
        val state = loadedState(toCurrency)
        model.updateNotEnoughFeeNotificationState(state)

        // Act
        model.setupBlankAmountUiState(fromSwapCurrencyStatus = mockk(relaxed = true))
        model.uiState = model.uiState.copy(notifications = persistentListOf(unableToCoverFee))
        model.updateNotEnoughFeeNotificationState(state)

        // Assert
        assertThat(notEnoughFeeEvents()).containsExactly(expectedEvent, expectedEvent)
    }

    @Test
    fun `GIVEN not enough fee reported WHEN pair changes THEN event sent for the new pair`() {
        // Arrange
        val model = modelShowing(unableToCoverFee)
        model.updateNotEnoughFeeNotificationState(loadedState(toCurrency))
        val otherToCurrency = currency(symbol = "SOL", networkName = "Solana", id = "solana")

        // Act
        model.updateNotEnoughFeeNotificationState(loadedState(otherToCurrency))

        // Assert
        assertThat(notEnoughFeeEvents()).containsExactly(expectedEvent, expectedEvent)
    }

    private fun notEnoughFeeEvents() = sentEvents.filterIsInstance<SwapEvents.NoticeNotEnoughFee>()

    private val expectedEvent
        get() = SwapEvents.NoticeNotEnoughFee(
            token = "USDT",
            blockchain = "Ethereum",
            balance = AnalyticsParam.TokenBalanceState.fromAmount(BigDecimal.TEN),
        )

    private fun modelShowing(notification: NotificationUM): SwapModel {
        val model = createModel()
        model.uiState = model.uiState.copy(notifications = persistentListOf(notification))
        return model
    }

    private fun loadedState(toCurrency: CryptoCurrency): SwapState.QuotesLoadedState {
        val fromStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
            every { currency } returns fromCurrency
            every { value.amount } returns BigDecimal.TEN
        }
        return mockk(relaxed = true) {
            every { fromTokenInfo.swapCurrencyStatus.status } returns fromStatus
            every { toTokenInfo.swapCurrencyStatus.currency } returns toCurrency
        }
    }

    private fun currency(symbol: String, networkName: String, id: String): CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.symbol } returns symbol
        every { network.name } returns networkName
        every { this@mockk.id.value } returns id
    }
}