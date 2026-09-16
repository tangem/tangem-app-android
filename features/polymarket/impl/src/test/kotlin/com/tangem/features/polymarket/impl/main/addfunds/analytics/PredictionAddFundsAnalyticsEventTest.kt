package com.tangem.features.polymarket.impl.main.addfunds.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Test

internal class PredictionAddFundsAnalyticsEventTest {

    @Test
    fun `GIVEN screen opened event WHEN built THEN category and name match the shared sheet`() {
        // Arrange
        val event = PredictionAddFundsAnalyticsEvent.MethodScreenOpened()

        // Act
        val actual = event.category to event.event

        // Assert
        assertThat(actual).isEqualTo("Add Funds" to "Method Screen Opened")
        assertThat(event.params).containsExactly(AnalyticsParam.SOURCE, "Predictions")
    }

    @Test
    fun `GIVEN button events WHEN built THEN names carry the Button prefix`() {
        // Arrange
        val swap = PredictionAddFundsAnalyticsEvent.ButtonSwap()
        val receive = PredictionAddFundsAnalyticsEvent.ButtonReceive()

        // Act
        val actual = listOf(swap.event, receive.event)

        // Assert
        assertThat(actual).containsExactly("Button - Swap", "Button - Receive").inOrder()
    }
}