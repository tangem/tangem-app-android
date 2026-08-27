package com.tangem.features.tangempay.orderCard.impl.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PhoneMaskFormatterTest {

    private val usMask = "+1 (###) ###-####"
    private val uaMask = "+380 (##) ###-####"
    private val kzMask = "+7 (###) ###-##-##"
    private val preprintedDigitMask = "+7 (7##) ###-##-##"

    @ParameterizedTest
    @MethodSource("applyCases")
    fun apply(model: MaskModel) {
        assertThat(PhoneMaskFormatter.apply(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("applyWithHintCases")
    fun applyWithHint(model: MaskModel) {
        assertThat(PhoneMaskFormatter.applyWithHint(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("e164Cases")
    fun toE164(model: MaskModel) {
        assertThat(PhoneMaskFormatter.toE164(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("completeCases")
    fun isComplete(model: CompleteModel) {
        assertThat(PhoneMaskFormatter.isComplete(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("placeholderCases")
    fun placeholderCount(model: PlaceholderModel) {
        assertThat(PhoneMaskFormatter.placeholderCount(model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("usableCases")
    fun isUsable(model: UsableModel) {
        assertThat(PhoneMaskFormatter.isUsable(model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("sanitizeStripsCountryCodeCases")
    fun sanitizeStripsCountryCode(model: MaskModel) {
        assertThat(PhoneMaskFormatter.sanitize(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("sanitizeKeepsNationalInputCases")
    fun sanitizeKeepsNationalInput(model: MaskModel) {
        assertThat(PhoneMaskFormatter.sanitize(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("sanitizeCapsAtMaskLengthCases")
    fun sanitizeCapsAtMaskLength(model: MaskModel) {
        assertThat(PhoneMaskFormatter.sanitize(model.raw, model.mask)).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("enteredLengthCases")
    fun enteredLength(model: MaskModel) {
        val entered = PhoneMaskFormatter.enteredLength(model.raw, model.mask)

        assertThat(PhoneMaskFormatter.applyWithHint(model.raw, model.mask).take(entered))
            .isEqualTo(model.expected)
    }

    internal data class MaskModel(val raw: String, val mask: String, val expected: String)
    internal data class CompleteModel(val raw: String, val mask: String, val expected: Boolean)
    internal data class PlaceholderModel(val mask: String, val expected: Int)
    internal data class UsableModel(val mask: String, val expected: Boolean)

    private fun applyCases() = listOf(
        MaskModel(raw = "2345678901", mask = usMask, expected = "+1 (234) 567-8901"),
        MaskModel(raw = "", mask = usMask, expected = ""),
        MaskModel(raw = "234", mask = usMask, expected = "+1 (234) "),
        MaskModel(raw = "2345", mask = "", expected = "2345"),
        MaskModel(raw = "501234567", mask = uaMask, expected = "+380 (50) 123-4567"),
        MaskModel(raw = "50", mask = uaMask, expected = "+380 (50) "),
        MaskModel(raw = "7012345678", mask = kzMask, expected = "+7 (701) 234-56-78"),
        MaskModel(raw = "012345678", mask = preprintedDigitMask, expected = "+7 (701) 234-56-78"),
    )

    private fun applyWithHintCases() = listOf(
        MaskModel(raw = "", mask = usMask, expected = "+1 (___) ___-____"),
        MaskModel(raw = "234", mask = usMask, expected = "+1 (234) ___-____"),
        MaskModel(raw = "2345678901", mask = usMask, expected = "+1 (234) 567-8901"),
        MaskModel(raw = "2345", mask = "", expected = "2345"),
        MaskModel(raw = "", mask = uaMask, expected = "+380 (__) ___-____"),
        MaskModel(raw = "50", mask = uaMask, expected = "+380 (50) ___-____"),
        MaskModel(raw = "7012345678", mask = kzMask, expected = "+7 (701) 234-56-78"),
    )

    private fun e164Cases() = listOf(
        MaskModel(raw = "2345678901", mask = usMask, expected = "+12345678901"),
        MaskModel(raw = "234", mask = usMask, expected = "+1234"),
        MaskModel(raw = "501234567", mask = uaMask, expected = "+380501234567"),
        MaskModel(raw = "50", mask = uaMask, expected = "+38050"),
        MaskModel(raw = "7012345678", mask = kzMask, expected = "+77012345678"),
    )

    private fun completeCases() = listOf(
        CompleteModel(raw = "2345678901", mask = usMask, expected = true),
        CompleteModel(raw = "234", mask = usMask, expected = false),
        CompleteModel(raw = "", mask = usMask, expected = false),
        CompleteModel(raw = "501234567", mask = uaMask, expected = true),
        CompleteModel(raw = "7012345678", mask = kzMask, expected = true),
        CompleteModel(raw = "1234567", mask = "", expected = true),
        CompleteModel(raw = "123456", mask = "", expected = false),
        CompleteModel(raw = "123456789012345", mask = "", expected = true),
        CompleteModel(raw = "1234567890123456", mask = "", expected = false),
        CompleteModel(raw = "", mask = "", expected = false),
        CompleteModel(raw = "1234567", mask = "(XXX) XXX", expected = true),
    )

    private fun usableCases() = listOf(
        UsableModel(mask = usMask, expected = true),
        UsableModel(mask = uaMask, expected = true),
        UsableModel(mask = kzMask, expected = true),
        UsableModel(mask = "", expected = false),
        UsableModel(mask = "(XXX) XXX", expected = false),
        UsableModel(mask = "(###) ###-####", expected = false),
        UsableModel(mask = "+1 (XXX)", expected = false),
    )

    private fun placeholderCases() = listOf(
        PlaceholderModel(mask = usMask, expected = 10),
        PlaceholderModel(mask = "", expected = 0),
        PlaceholderModel(mask = "(XXX) XXX", expected = 0),
        PlaceholderModel(mask = "###", expected = 3),
    )

    private fun sanitizeStripsCountryCodeCases() = listOf(
        MaskModel(raw = "+1 (234) 567-8901", mask = usMask, expected = "2345678901"),
        MaskModel(raw = "+380 (50) 123-4567", mask = uaMask, expected = "501234567"),
        MaskModel(raw = "+77012345678", mask = preprintedDigitMask, expected = "012345678"),
    )

    private fun sanitizeKeepsNationalInputCases() = listOf(
        MaskModel(raw = "(234) 567", mask = usMask, expected = "234567"),
        MaskModel(raw = "2345678901", mask = usMask, expected = "2345678901"),
        MaskModel(raw = "501234567", mask = uaMask, expected = "501234567"),
        MaskModel(raw = "١٢٣٤٥", mask = usMask, expected = "12345"),
        MaskModel(raw = "12३45", mask = usMask, expected = "12345"),
        MaskModel(raw = "２３４", mask = usMask, expected = "234"),
    )

    private fun sanitizeCapsAtMaskLengthCases() = listOf(
        MaskModel(raw = "12345678901", mask = usMask, expected = "1234567890"),
        MaskModel(raw = "23456789012345", mask = usMask, expected = "2345678901"),
        MaskModel(raw = "70123456789", mask = kzMask, expected = "7012345678"),
        MaskModel(raw = "1234567890123456789", mask = "", expected = "123456789012345"),
    )

    private fun enteredLengthCases() = listOf(
        MaskModel(raw = "", mask = usMask, expected = "+1 "),
        MaskModel(raw = "234", mask = usMask, expected = "+1 (234) "),
        MaskModel(raw = "2345678901", mask = usMask, expected = "+1 (234) 567-8901"),
        MaskModel(raw = "", mask = "###-###", expected = ""),
        MaskModel(raw = "", mask = uaMask, expected = "+380 "),
        MaskModel(raw = "50", mask = uaMask, expected = "+380 (50) "),
    )
}