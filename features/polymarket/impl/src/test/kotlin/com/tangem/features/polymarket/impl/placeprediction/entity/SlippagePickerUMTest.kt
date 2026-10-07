package com.tangem.features.polymarket.impl.placeprediction.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SlippagePickerUMTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an entered value WHEN parsed THEN accepts only the 3-30 percent range`(case: OfCase) {
        // Act
        val actual = SlippagePickerUM.of(value = case.value)

        // Assert
        assertThat(actual.error).isEqualTo(case.expectedError)
        assertThat(actual.isConfirmEnabled).isEqualTo(case.expectedConfirmEnabled)
    }

    @Test
    fun `GIVEN the presets WHEN listed THEN they are the shared scale with the default first`() {
        // Act
        val actual = SLIPPAGE_PRESETS.map { it.percent.toPlainString() to it.role }

        // Assert
        assertThat(actual).containsExactly(
            "3" to SlippagePresetUM.Role.Default,
            "5" to SlippagePresetUM.Role.Plain,
            "10" to SlippagePresetUM.Role.Plain,
            "15" to SlippagePresetUM.Role.Plain,
            "30" to SlippagePresetUM.Role.Plain,
        ).inOrder()
    }

    private fun provideTestModels() = listOf(
        OfCase(value = "", expectedError = null, expectedConfirmEnabled = false),
        OfCase(value = "2.99", expectedError = belowMinError(), expectedConfirmEnabled = false),
        OfCase(value = "0.25", expectedError = belowMinError(), expectedConfirmEnabled = false),
        OfCase(value = "3", expectedError = null, expectedConfirmEnabled = true),
        OfCase(value = "12.5", expectedError = null, expectedConfirmEnabled = true),
        OfCase(value = "30", expectedError = null, expectedConfirmEnabled = true),
        OfCase(
            value = "30.01",
            expectedError = resourceReference(R.string.prediction_place_slippage_error),
            expectedConfirmEnabled = false,
        ),
    )

    private fun belowMinError(): TextReference = resourceReference(
        id = R.string.prediction_place_slippage_min_error,
        formatArgs = wrappedList(BigDecimal("3").formatSlippagePercent()),
    )

    data class OfCase(val value: String, val expectedError: TextReference?, val expectedConfirmEnabled: Boolean)
}