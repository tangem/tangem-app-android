package com.tangem.features.tangempay.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.text.Normalizer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayFormCharsetTest {

    @ParameterizedTest
    @MethodSource("embossCases")
    fun isEmbossCharsetValid(model: CharsetModel) {
        assertThat(TangemPayFormCharset.isEmbossCharsetValid(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("recipientNameCases")
    fun isRecipientNameCharsetValid(model: CharsetModel) {
        assertThat(TangemPayFormCharset.isRecipientNameCharsetValid(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("shippingCases")
    fun isShippingCharsetValid(model: CharsetModel) {
        assertThat(TangemPayFormCharset.isShippingCharsetValid(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("postalCodeCases")
    fun isPostalCodeCharsetValid(model: CharsetModel) {
        assertThat(TangemPayFormCharset.isPostalCodeCharsetValid(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("addressCases")
    fun isAddressCharsetValid(model: CharsetModel) {
        assertThat(TangemPayFormCharset.isAddressCharsetValid(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("nonLatinLetterCases")
    fun containsNonLatinLetter(model: CharsetModel) {
        assertThat(TangemPayFormCharset.containsNonLatinLetter(model.input)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("normalizeCases")
    fun normalize(model: NormalizeModel) {
        assertThat(TangemPayFormCharset.normalize(model.input)).isEqualTo(model.expected)
    }

    internal data class CharsetModel(val input: String, val expected: Boolean)

    internal data class NormalizeModel(val input: String, val expected: String)

    private fun nfd(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)

    private fun embossCases() = listOf(
        CharsetModel(input = "JOHN DOE", expected = true),
        CharsetModel(input = "Mary-Jane Smith Jr.", expected = true),
        CharsetModel(input = "O'BRIEN", expected = false),
        CharsetModel(input = "O’BRIEN", expected = false),
        CharsetModel(input = "O‘BRIEN", expected = false),
        CharsetModel(input = "JOHN\u00A0DOE", expected = true),
        CharsetModel(input = "José", expected = false),
        CharsetModel(input = "Москва", expected = false),
        CharsetModel(input = "", expected = true),
        CharsetModel(input = "John 42", expected = true),
        CharsetModel(input = "Doe, John", expected = false),
        CharsetModel(input = "Doe/Smith", expected = false),
        CharsetModel(input = "John (Jr)", expected = false),
        CharsetModel(input = "John: Doe", expected = false),
        CharsetModel(input = "John #Doe", expected = false),
        CharsetModel(input = "Smith & Sons", expected = false),
        CharsetModel(input = "John_Doe", expected = false),
    )

    private fun recipientNameCases() = listOf(
        CharsetModel(input = "Johnny", expected = true),
        CharsetModel(input = "Mary Jane", expected = true),
        CharsetModel(input = "Jean-Luc", expected = true),
        CharsetModel(input = "", expected = true),
        CharsetModel(input = "O'Brien", expected = false),
        CharsetModel(input = "O’Brien", expected = false),
        CharsetModel(input = "St. John", expected = false),
        CharsetModel(input = "John 2", expected = false),
        CharsetModel(input = "Doe, John", expected = false),
        CharsetModel(input = "José", expected = false),
        CharsetModel(input = "Иван", expected = false),
    )

    private fun shippingCases() = listOf(
        CharsetModel(input = "JOHN Doe 42", expected = true),
        CharsetModel(input = "Apt 5/7", expected = true),
        CharsetModel(input = "Saint-Denis", expected = true),
        CharsetModel(input = "St. John", expected = true),
        CharsetModel(input = "Suite 5, Floor 2", expected = true),
        CharsetModel(input = "O'Brien", expected = true),
        CharsetModel(input = "Block (A)", expected = true),
        CharsetModel(input = "Apt #5", expected = true),
        CharsetModel(input = "Smith & Sons", expected = true),
        CharsetModel(input = "Calle 5 N°12", expected = true),
        CharsetModel(input = "O’BRIEN", expected = true),
        CharsetModel(input = "O‘BRIEN", expected = true),
        CharsetModel(input = "JOHN\u00A0DOE", expected = true),
        CharsetModel(input = "", expected = true),
        CharsetModel(input = "José", expected = false),
        CharsetModel(input = "São Paulo", expected = false),
        CharsetModel(input = nfd("São Paulo"), expected = false),
        CharsetModel(input = "Kraków", expected = false),
        CharsetModel(input = "Müller Straße 10", expected = false),
        CharsetModel(input = "Đà Nẵng", expected = false),
        CharsetModel(input = "İstanbul", expected = false),
        CharsetModel(input = "Москва", expected = false),
        CharsetModel(input = "北京", expected = false),
        CharsetModel(input = "Unit: 10", expected = false),
        CharsetModel(input = "john@doe", expected = false),
        CharsetModel(input = "Unit\t10", expected = false),
    )

    private fun postalCodeCases() = listOf(
        CharsetModel(input = "94103", expected = true),
        CharsetModel(input = "SW1A 1AA", expected = true),
        CharsetModel(input = "1000-001", expected = true),
        CharsetModel(input = "", expected = true),
        CharsetModel(input = "123/45", expected = false),
        CharsetModel(input = "12.345", expected = false),
        CharsetModel(input = "#12345", expected = false),
        CharsetModel(input = "Ü1234", expected = false),
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

    private fun nonLatinLetterCases() = listOf(
        CharsetModel(input = "José", expected = true),
        CharsetModel(input = nfd("São Paulo"), expected = true),
        CharsetModel(input = "q${Char(0x0303)}", expected = true),
        CharsetModel(input = "Müller Straße 10", expected = true),
        CharsetModel(input = "Москва", expected = true),
        CharsetModel(input = "北京", expected = true),
        CharsetModel(input = "O'Brien", expected = false),
        CharsetModel(input = "Unit: 10", expected = false),
        CharsetModel(input = "john@doe", expected = false),
        CharsetModel(input = "Calle 5 N°12", expected = false),
        CharsetModel(input = "A×B", expected = false),
        CharsetModel(input = "", expected = false),
    )

    private fun normalizeCases() = listOf(
        NormalizeModel(input = "O’Brien", expected = "O'Brien"),
        NormalizeModel(input = "O‘Brien", expected = "O'Brien"),
        NormalizeModel(input = "Crescent\u00A0st. 24", expected = "Crescent st. 24"),
        NormalizeModel(input = nfd("São Paulo"), expected = "São Paulo"),
        NormalizeModel(input = "Night City", expected = "Night City"),
    )
}