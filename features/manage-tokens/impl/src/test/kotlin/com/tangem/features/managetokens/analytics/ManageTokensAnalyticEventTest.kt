package com.tangem.features.managetokens.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.managetokens.component.ManageTokensSource
import org.junit.jupiter.api.Test

/**
 * Pins the OTel allowlist contract of the marked events in this module: every allowed param must
 * exist on the event, and every non-exported param is deliberately dropped.
 */
internal class ManageTokensAnalyticEventTest {

    @Test
    fun `GIVEN token added event WHEN params classified THEN only source is exported`() {
        // Arrange
        val event = ManageTokensAnalyticEvent.TokenAdded(tokensCount = 2, source = ManageTokensSource.WALLET)

        // Act
        val allowedParams = event.otelMetric.allowedParams

        // Assert
        assertThat(allowedParams).containsExactly(AnalyticsParam.Key.SOURCE)
        assertThat(event.params.keys - allowedParams).containsExactly(AnalyticsParam.Key.COUNT)
    }
}