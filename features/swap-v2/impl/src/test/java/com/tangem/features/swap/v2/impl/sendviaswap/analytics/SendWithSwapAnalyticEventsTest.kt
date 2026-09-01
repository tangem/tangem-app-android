package com.tangem.features.swap.v2.impl.sendviaswap.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_TOKEN
import com.tangem.core.analytics.models.OtelIncludedEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * Pins the OTel allowlist contract of the marked events in this module: every allowed param must
 * exist on the event, and every non-exported param is deliberately dropped.
 */
internal class SendWithSwapAnalyticEventsTest {

    private val network = mockk<Network>(relaxed = true).also {
        every { it.name } returns "Ethereum"
    }
    private val fromToken = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns "ETH"
        every { it.network } returns network
    }

    @Test
    fun `GIVEN insufficient balance event WHEN params classified THEN only send token is exported`() {
        // Arrange
        every { fromToken.isCustom } returns false
        val event = SendWithSwapAnalyticEvents.ErrorInsufficientBalance(fromToken)

        // Act
        val allowedParams = event.otelMetric.allowedParams

        // Assert
        assertThat(allowedParams).containsExactly(SEND_TOKEN)
        assertThat(event.params.keys - allowedParams).containsExactly(SEND_BLOCKCHAIN)
        assertThat(event.otelParamOverrides).isEmpty()
    }

    @Test
    fun `GIVEN custom from token WHEN insufficient balance event THEN send token exported as bounded stand-in`() {
        // Arrange
        every { fromToken.isCustom } returns true

        // Act
        val event = SendWithSwapAnalyticEvents.ErrorInsufficientBalance(fromToken)

        // Assert
        assertThat(event.otelParamOverrides).containsExactly(SEND_TOKEN, OtelIncludedEvent.CUSTOM_TOKEN)
        assertThat(event.params[SEND_TOKEN]).isEqualTo("ETH")
    }
}