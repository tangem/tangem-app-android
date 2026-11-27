package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EarnNoFundsNoticeAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk()

    private val lending = EarnNoFundsNoticeAnalytics.Notice(token = "ETH", product = "Lending")
    private val vault = EarnNoFundsNoticeAnalytics.Notice(token = "ETH", product = "Vault")

    private val analytics = EarnNoFundsNoticeAnalytics(analyticsEventHandler)

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsEventHandler)
        every { analyticsEventHandler.send(any()) } just Runs
    }

    @Test
    fun `GIVEN no notice reported WHEN onCta with Lending THEN one no funds notice sent`() {
        // Act
        analytics.onCta(lending)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice("ETH", "Lending")) }
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN Lending reported WHEN onCta with Lending again THEN nothing new sent`() {
        // Arrange
        analytics.onCta(lending)

        // Act
        analytics.onCta(lending)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice("ETH", "Lending")) }
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN Lending reported WHEN onCta with null then Lending THEN notice sent again`() {
        // Arrange
        analytics.onCta(lending)

        // Act
        analytics.onCta(null)
        analytics.onCta(lending)

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice("ETH", "Lending")) }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN Lending reported WHEN onCta with Vault THEN notice sent for Vault`() {
        // Arrange
        analytics.onCta(lending)

        // Act
        analytics.onCta(vault)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice("ETH", "Lending")) }
        verify(exactly = 1) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice("ETH", "Vault")) }
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN no notice reported WHEN onCta with null THEN nothing sent`() {
        // Act
        analytics.onCta(null)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }
}