package com.tangem.core.ui.components.fields

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PinTextFieldTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun acceptedPin(model: AcceptedPinModel) {
        // Act
        val actual = acceptedPin(raw = model.raw, length = LENGTH, current = model.current)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class AcceptedPinModel(val raw: String, val current: String, val expected: String)

    private fun provideTestModels() = listOf(
        AcceptedPinModel(raw = "1234", current = "123", expected = "1234"),
        AcceptedPinModel(raw = "123a", current = "123", expected = "123"),
        AcceptedPinModel(raw = "12ab34", current = "12", expected = "1234"),
        AcceptedPinModel(raw = "12٣4", current = "12", expected = "124"),
        AcceptedPinModel(raw = "1234567", current = "123456", expected = "123456"),
        AcceptedPinModel(raw = "", current = "12", expected = ""),
    )

    private companion object {
        const val LENGTH = 6
    }
}