package com.tangem.features.tokenreceive.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReceiveAddressHighlightTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN address WHEN receiveAddressDimmedRange THEN short addresses stay in full color and long ones dim the middle`(
        model: HighlightModel,
    ) {
        // Act
        val actual = receiveAddressDimmedRange(model.address)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        HighlightModel(address = "", expected = null),
        HighlightModel(address = "0.0.30322", expected = null),
        HighlightModel(address = "0.0.1234567890", expected = null),
        HighlightModel(address = "0.0.12345678901", expected = 5..9),
        HighlightModel(address = "0x3987734c3379A3E2e3eF21c3C8090Cc7aBCD6260", expected = 5..36),
    )

    data class HighlightModel(val address: String, val expected: IntRange?)
}