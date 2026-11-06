package com.tangem.features.send.api.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class CommonSendAnalyticEventsTest {

    @Test
    fun `GIVEN Earning source WHEN fee token screen opened THEN id and params match the spec`() {
        // Act
        val event = CommonSendAnalyticEvents.FeeTokenScreenOpened(
            categoryName = "Earning",
            source = CommonSendAnalyticEvents.CommonSendSource.Earning,
            availableTokens = "POL, USDC",
            blockchain = "Polygon",
        )

        // Assert
        assertThat(event.id).isEqualTo("[Earning] Fee Token Screen Opened")
        assertThat(event.params).isEqualTo(
            mapOf(
                "Source" to "Earning",
                "Available Fee" to "POL, USDC",
                "Blockchain" to "Polygon",
            ),
        )
    }
}