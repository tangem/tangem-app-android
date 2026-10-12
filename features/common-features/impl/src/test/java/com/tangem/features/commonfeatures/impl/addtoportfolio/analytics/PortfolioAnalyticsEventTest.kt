package com.tangem.features.commonfeatures.impl.addtoportfolio.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Test

/**
 * Pins the OTel allowlist contract of the marked events in this module: every allowed param must
 * exist on the event, and every non-exported param is deliberately dropped.
 */
internal class PortfolioAnalyticsEventTest {

    @Test
    fun `GIVEN token added event WHEN params classified THEN only source is exported`() {
        // Arrange
        val event = PortfolioAnalyticsEvent.EventBuilder(
            tokenSymbol = "BTC",
            source = "Market",
            category = "Markets / Chart",
        ).tokenAdded(blockchainName = "Bitcoin")

        // Act
        val allowedParams = event.otelMetric.allowedParams

        // Assert
        assertThat(allowedParams).containsExactly(AnalyticsParam.SOURCE)
        assertThat(event.params.keys - allowedParams)
            .containsExactly(AnalyticsParam.TOKEN_PARAM, AnalyticsParam.BLOCKCHAIN)
    }
}