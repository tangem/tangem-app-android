package com.tangem.core.analytics.models.event

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class ManageFundsAnalyticsEventTest {

    @Test
    fun `GIVEN the Earn source WHEN MethodScreenOpened built THEN the event matches the spec`() {
        // Act
        val actual = ManageFundsAnalyticsEvent.MethodScreenOpened(source = AnalyticsParam.ScreensSources.Earn)

        // Assert
        assertThat(actual.id).isEqualTo("[Add Funds] Method Screen Opened")
        assertThat(actual.params).isEqualTo(mapOf("Source" to "Earn"))
    }

    @ParameterizedTest
    @MethodSource("buttons")
    fun `GIVEN a method button WHEN built THEN the event matches the spec`(actual: AnalyticsEvent, expectedId: String) {
        // Assert
        assertThat(actual.id).isEqualTo(expectedId)
        assertThat(actual.params).isEmpty()
    }

    private companion object {

        @JvmStatic
        fun buttons() = listOf(
            Arguments.of(ManageFundsAnalyticsEvent.ButtonBuy(), "[Add Funds] Button - Buy"),
            Arguments.of(ManageFundsAnalyticsEvent.ButtonSwap(), "[Add Funds] Button - Swap"),
            Arguments.of(ManageFundsAnalyticsEvent.ButtonReceive(), "[Add Funds] Button - Receive"),
        )
    }
}