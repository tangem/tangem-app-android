package com.tangem.feature.swap.models

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.models.states.SwapNotificationUM
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PredictionTopUpLimitsTest {

    @ParameterizedTest
    @ValueSource(strings = ["0.5", "0.999999"])
    fun `GIVEN amount below minimum WHEN warningFor THEN minimum warning`(amount: String) {
        // Act
        val actual = LIMITS.warningFor(BigDecimal(amount))

        // Assert
        assertThat(actual).isEqualTo(SwapNotificationUM.Warning.PredictionMinimumDeposit(amountText = "$1.00"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["100", "150"])
    fun `GIVEN amount at or above maximum WHEN warningFor THEN maximum warning`(amount: String) {
        // Act
        val actual = LIMITS.warningFor(BigDecimal(amount))

        // Assert
        assertThat(actual).isEqualTo(SwapNotificationUM.Warning.PredictionMaximumDeposit(amountText = "$100.00"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "1.5", "99.999999", "0", "-1"])
    fun `GIVEN amount within limits or not positive WHEN warningFor THEN no warning`(amount: String) {
        // Act
        val actual = LIMITS.warningFor(BigDecimal(amount))

        // Assert
        assertThat(actual).isNull()
    }

    private companion object {
        val LIMITS = PredictionTopUpLimits(
            minimum = BigDecimal.ONE,
            minimumText = "$1.00",
            maximum = BigDecimal(100),
            maximumText = "$100.00",
        )
    }
}