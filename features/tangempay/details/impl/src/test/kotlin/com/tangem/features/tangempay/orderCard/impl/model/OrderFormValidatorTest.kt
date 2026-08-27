package com.tangem.features.tangempay.orderCard.impl.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.text.Normalizer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderFormValidatorTest {

    @ParameterizedTest
    @MethodSource("embossCases")
    fun isEmbossCharsetValid(model: CharsetModel) {
        assertThat(OrderFormValidator.isEmbossCharsetValid(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("addressCases")
    fun isAddressCharsetValid(model: CharsetModel) {
        assertThat(OrderFormValidator.isAddressCharsetValid(model.input)).isEqualTo(model.expected)
    }

    internal data class CharsetModel(val input: String, val expected: Boolean)

    private fun nfd(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)

    private fun embossCases() = listOf(
        CharsetModel(input = "JOHN DOE", expected = true),
        CharsetModel(input = "O'Brien-42 (Jr.)", expected = true),
        CharsetModel(input = "O’BRIEN", expected = true),
        CharsetModel(input = "O‘BRIEN", expected = true),
        CharsetModel(input = "JOHN\u00A0DOE", expected = true),
        CharsetModel(input = "José", expected = false),
        CharsetModel(input = "Москва", expected = false),
        CharsetModel(input = "", expected = true),
    )

    private fun addressCases() = listOf(
        CharsetModel(input = "São Paulo", expected = true),
        CharsetModel(input = "Île-de-France", expected = true),
        CharsetModel(input = "Kraków", expected = true),
        CharsetModel(input = "Müller Straße 10", expected = true),
        CharsetModel(input = "București", expected = true),
        CharsetModel(input = "Đà Nẵng", expected = true),
        CharsetModel(input = "İstanbul", expected = true),
        CharsetModel(input = nfd("São Paulo"), expected = true),
        CharsetModel(input = nfd("Đà Nẵng"), expected = true),
        CharsetModel(input = "O’Brien Street", expected = true),
        CharsetModel(input = "O‘Brien Street", expected = true),
        CharsetModel(input = "M\u00fcller\u00A0Stra\u00dfe 10", expected = true),
        CharsetModel(input = "Smith & Sons Bldg", expected = true),
        CharsetModel(input = "Москва", expected = false),
        CharsetModel(input = "北京", expected = false),
        CharsetModel(input = "A×B", expected = false),
        CharsetModel(input = "A÷B", expected = false),
        CharsetModel(input = "", expected = true),
    )
}