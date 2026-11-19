package com.tangem.core.analytics.models.event

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class NetworkFeeUnavailableAnalyticsEventTest {

    @ParameterizedTest
    @CsvSource(
        "Send, Send",
        "Swap, Swap",
        "Staking, Staking",
        "WalletConnect, WalletConnect",
        "Earning, Earning",
    )
    fun `GIVEN a source WHEN event built THEN the event matches the spec`(
        source: NetworkFeeUnavailableAnalyticsEvent.Source,
        expectedSource: String,
    ) {
        // Act
        val actual = NetworkFeeUnavailableAnalyticsEvent(source)

        // Assert
        assertThat(actual.id).isEqualTo("[Notice] Network Fee Unavailable")
        assertThat(actual.params).isEqualTo(mapOf("Source" to expectedSource))
    }
}