package com.tangem.features.onramp.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.test.core.ProvideTestModels
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampProviderCalculatedAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN quotes WHEN send THEN provider calculated sent for best-rate provider`(model: SelectionModel) {
        // Act
        analyticsEventHandler.sendProviderCalculatedEvent(quotes = model.quotes, tokenSymbol = TOKEN_SYMBOL)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.ProviderCalculated(
                    providerName = model.expectedProviderName,
                    tokenSymbol = TOKEN_SYMBOL,
                    paymentMethod = PAYMENT_METHOD,
                ),
            )
        }
    }

    @Test
    fun `GIVEN no quotes WHEN send THEN no event sent`() {
        // Act
        analyticsEventHandler.sendProviderCalculatedEvent(quotes = emptyList(), tokenSymbol = TOKEN_SYMBOL)

        // Assert
        verify { analyticsEventHandler wasNot Called }
    }

    @Test
    fun `GIVEN only non-loaded quotes WHEN send THEN no event sent`() {
        // Arrange
        val quotes = listOf(mockk<OnrampQuote.Error>(), mockk<OnrampQuote.AmountError>())

        // Act
        analyticsEventHandler.sendProviderCalculatedEvent(quotes = quotes, tokenSymbol = TOKEN_SYMBOL)

        // Assert
        verify { analyticsEventHandler wasNot Called }
    }

    private fun provideTestModels() = listOf(
        SelectionModel(
            name = "highest-rate quote among several is selected",
            quotes = listOf(
                createQuote(providerName = "Low", rate = BigDecimal("100")),
                createQuote(providerName = "High", rate = BigDecimal("120")),
                createQuote(providerName = "Mid", rate = BigDecimal("90")),
            ),
            expectedProviderName = "High",
        ),
        SelectionModel(
            name = "SEPA quote with lower rate is NOT prioritized, higher-rate quote wins",
            quotes = listOf(
                createQuote(providerName = "SepaLowerRate", rate = BigDecimal("100")),
                createQuote(providerName = "CardHigherRate", rate = BigDecimal("105")),
            ),
            expectedProviderName = "CardHigherRate",
        ),
        SelectionModel(
            name = "single loaded quote is selected",
            quotes = listOf(
                createQuote(providerName = "Single", rate = BigDecimal("100")),
            ),
            expectedProviderName = "Single",
        ),
        SelectionModel(
            name = "best-rate loaded quote is selected even when error quotes are present",
            quotes = listOf(
                mockk<OnrampQuote.Error>(),
                createQuote(providerName = "Loaded", rate = BigDecimal("100")),
                mockk<OnrampQuote.AmountError>(),
            ),
            expectedProviderName = "Loaded",
        ),
    )

    private fun createQuote(providerName: String, rate: BigDecimal): OnrampQuote.Data {
        return mockk<OnrampQuote.Data> {
            every { provider.info.name } returns providerName
            every { paymentMethod.name } returns PAYMENT_METHOD
            every { toAmount.value } returns rate
        }
    }

    internal data class SelectionModel(
        val name: String,
        val quotes: List<OnrampQuote>,
        val expectedProviderName: String,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val TOKEN_SYMBOL = "BTC"
        const val PAYMENT_METHOD = "Card"
    }
}