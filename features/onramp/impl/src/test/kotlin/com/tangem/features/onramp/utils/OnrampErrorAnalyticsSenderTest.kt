package com.tangem.features.onramp.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.test.core.ProvideTestModels
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN non analytics error WHEN send THEN nothing is sent`(error: OnrampError) {
        // Act
        analyticsEventHandler.sendOnrampErrorEvent(
            error = error,
            tokenSymbol = "BTC",
        )

        // Assert
        verify { analyticsEventHandler wasNot Called }
    }

    private fun provideTestModels() = listOf(
        OnrampError.PairsNotFound,
        OnrampError.AlreadyHandledTransaction,
        OnrampError.RedirectError.VerificationFailed,
        OnrampError.RedirectError.WrongRequestId,
        OnrampError.AmountError.TooBigError(BigDecimal.TEN),
        OnrampError.AmountError.TooSmallError(BigDecimal.ONE),
    )

    @Test
    fun `GIVEN null domain description WHEN send THEN app error uses empty description`() {
        // Arrange
        val error = OnrampError.DomainError(null)

        // Act
        analyticsEventHandler.sendOnrampErrorEvent(error, "BTC")

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(OnrampAnalyticsEvent.AppErrors("BTC", ""))
        }
    }
}