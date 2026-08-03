package com.tangem.features.tangempay.orderCard.impl.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextInputFocusTest {

    @ParameterizedTest
    @MethodSource("focusCases")
    fun shouldReportFocusChange(model: FocusModel) {
        assertThat(shouldReportFocusChange(wasFocused = model.wasFocused, isFocused = model.isFocused))
            .isEqualTo(model.expected)
    }

    internal data class FocusModel(val wasFocused: Boolean?, val isFocused: Boolean, val expected: Boolean)

    private fun focusCases() = listOf(
        FocusModel(wasFocused = null, isFocused = false, expected = false),
        FocusModel(wasFocused = null, isFocused = true, expected = true),
        FocusModel(wasFocused = false, isFocused = false, expected = false),
        FocusModel(wasFocused = false, isFocused = true, expected = true),
        FocusModel(wasFocused = true, isFocused = true, expected = false),
        FocusModel(wasFocused = true, isFocused = false, expected = true),
    )
}