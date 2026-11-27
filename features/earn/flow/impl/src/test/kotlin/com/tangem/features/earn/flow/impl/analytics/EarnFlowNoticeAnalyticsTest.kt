package com.tangem.features.earn.flow.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EarnFlowNoticeAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk()

    private val analytics = EarnFlowNoticeAnalytics(
        analyticsEventHandler = analyticsEventHandler,
        category = "ETH Lending",
    )

    private fun balanceEvent(screen: EarnFlowScreen) =
        EarnFlowAnalyticsEvent.InsufficientBalance(category = "ETH Lending", screen = screen)

    private fun feeEvent(screen: EarnFlowScreen) =
        EarnFlowAnalyticsEvent.InsufficientFee(category = "ETH Lending", screen = screen)

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsEventHandler)
        every { analyticsEventHandler.send(any()) } just Runs
    }

    @Test
    fun `GIVEN the same notice twice WHEN update THEN one event`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientBalance))
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientBalance))

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(balanceEvent(EarnFlowScreen.Deposit))
        }
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a notice gone and back WHEN update THEN two events`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Deposit, emptySet())
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 2) {
            analyticsEventHandler.send(feeEvent(EarnFlowScreen.Deposit))
        }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a notice on Deposit then on Confirm WHEN update THEN two events`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Confirm, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(feeEvent(EarnFlowScreen.Deposit))
        }
        verify(exactly = 1) {
            analyticsEventHandler.send(feeEvent(EarnFlowScreen.Confirm))
        }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN balance notice shown WHEN fee notice joins THEN only the fee event is added`() {
        // Arrange
        analytics.update(EarnFlowScreen.Withdraw, setOf(EarnFlowNotice.InsufficientBalance))

        // Act
        analytics.update(
            EarnFlowScreen.Withdraw,
            setOf(EarnFlowNotice.InsufficientBalance, EarnFlowNotice.InsufficientFee),
        )

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(balanceEvent(EarnFlowScreen.Withdraw))
        }
        verify(exactly = 1) {
            analyticsEventHandler.send(feeEvent(EarnFlowScreen.Withdraw))
        }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a notice on Confirm WHEN the user leaves and comes back with it still present THEN it is sent again`() {
        // Act
        analytics.update(EarnFlowScreen.Confirm, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Deposit, emptySet())
        analytics.update(EarnFlowScreen.Confirm, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Confirm)) }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a notice on Deposit WHEN the screens alternate THEN every visit sends it once`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Confirm, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Confirm, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Deposit)) }
        verify(exactly = 1) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Confirm)) }
        verify(exactly = 3) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a fee notice WHEN the fee is in flight and the notice is back THEN one event`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Deposit, emptySet(), isFeeInFlight = true)
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Deposit)) }
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a fee notice WHEN in flight and not back THEN it is sent after a later one`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Deposit, emptySet(), isFeeInFlight = true)
        analytics.update(EarnFlowScreen.Deposit, emptySet())
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Deposit)) }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN no fee notice WHEN the fee is in flight THEN nothing is sent`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, emptySet(), isFeeInFlight = true)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a fee notice on Deposit WHEN the fee is in flight on Confirm THEN the notice is not carried over`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientFee))
        analytics.update(EarnFlowScreen.Confirm, emptySet(), isFeeInFlight = true)
        analytics.update(EarnFlowScreen.Confirm, setOf(EarnFlowNotice.InsufficientFee))

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Deposit)) }
        verify(exactly = 1) { analyticsEventHandler.send(feeEvent(EarnFlowScreen.Confirm)) }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN a balance notice WHEN the fee is in flight THEN the balance notice is not carried over`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientBalance))
        analytics.update(EarnFlowScreen.Deposit, emptySet(), isFeeInFlight = true)
        analytics.update(EarnFlowScreen.Deposit, setOf(EarnFlowNotice.InsufficientBalance))

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(balanceEvent(EarnFlowScreen.Deposit)) }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN no notices WHEN update THEN nothing is sent`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, emptySet())

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }
}