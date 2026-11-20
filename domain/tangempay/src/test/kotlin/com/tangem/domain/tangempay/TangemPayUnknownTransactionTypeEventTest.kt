package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import org.junit.jupiter.api.Test

internal class TangemPayUnknownTransactionTypeEventTest {

    @Test
    fun `GIVEN wire type WHEN event created THEN it is an Errors event carrying the type`() {
        // Act
        val event = TangemPayAnalyticsEvents.UnknownTransactionType(type = "transfer")

        // Assert
        assertThat(event.category).isEqualTo("Visa Screen")
        assertThat(event.event).isEqualTo("Errors")
        assertThat(event.params).containsExactly(
            AnalyticsParam.ERROR_DESCRIPTION, "Unknown Transaction Type",
            AnalyticsParam.Key.TYPE, "transfer",
        )
    }

    @Test
    fun `GIVEN missing wire type WHEN event created THEN the type param says so`() {
        // Act
        val event = TangemPayAnalyticsEvents.UnknownTransactionType(type = null)

        // Assert
        assertThat(event.params[AnalyticsParam.Key.TYPE]).isEqualTo("missing")
    }
}