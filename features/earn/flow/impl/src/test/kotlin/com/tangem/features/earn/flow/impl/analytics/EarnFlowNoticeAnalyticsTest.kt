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
    fun `GIVEN no notices WHEN update THEN nothing is sent`() {
        // Act
        analytics.update(EarnFlowScreen.Deposit, emptySet())

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }
}