package com.tangem.features.onramp.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.error.OnrampError
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OnrampErrorAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsEventHandler)
    }

    @Test
    fun `GIVEN data error WHEN send THEN errors event contains provider context`() {
        // Arrange
        val error = OnrampError.DataError(code = "provider_error", description = "failed")

        // Act
        analyticsEventHandler.sendOnrampErrorEvent(
            error = error,
            tokenSymbol = "BTC",
            providerName = "Provider",
            paymentMethod = "Card",
        )

        // Assert
        verify {
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.Errors(
                    tokenSymbol = "BTC",
                    providerName = "Provider",
                    paymentMethod = "Card",
                    errorCode = "provider_error",
                ),
            )
        }
    }

    @Test
    fun `GIVEN domain error WHEN send THEN app error contains description`() {
        // Arrange
        val error = OnrampError.DomainError(description = "Unexpected response")

        // Act
        analyticsEventHandler.sendOnrampErrorEvent(error = error, tokenSymbol = "ETH")

        // Assert
        verify {
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.AppErrors(
                    tokenSymbol = "ETH",
                    errorDescription = "Unexpected response",
                ),
            )
        }
    }

    @Test
    fun `GIVEN non analytics error WHEN send THEN nothing is sent`() {
        // Act
        analyticsEventHandler.sendOnrampErrorEvent(
            error = OnrampError.PairsNotFound,
            tokenSymbol = "BTC",
        )

        // Assert
        verify { analyticsEventHandler wasNot Called }
    }
}