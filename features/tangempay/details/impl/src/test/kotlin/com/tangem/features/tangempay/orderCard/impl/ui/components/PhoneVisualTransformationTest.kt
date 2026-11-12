package com.tangem.features.tangempay.orderCard.impl.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PhoneVisualTransformationTest {

    private val usMask = "+1 (###) ###-####"
    private val enteredColor = Color.White
    private val hintColor = Color.Gray

    private fun transformation(mask: String = usMask) = PhoneVisualTransformation(
        mask = mask,
        enteredColor = enteredColor,
        hintColor = hintColor,
    )

    @ParameterizedTest
    @MethodSource("maskedTextCases")
    fun `GIVEN digits WHEN filter THEN entered part is masked and the rest of the mask stays a hint`(
        model: MaskedTextModel,
    ) {
        // Act
        val result = transformation().filter(AnnotatedString(model.raw))

        // Assert
        assertThat(result.text.text).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("spanStyleCases")
    fun `GIVEN digits WHEN filter THEN entered and hint parts carry their own colours`(model: SpanStyleModel) {
        // Act
        val result = transformation().filter(AnnotatedString(model.raw))

        // Assert
        assertThat(result.text.spanStyles).containsExactlyElementsIn(model.expected).inOrder()
    }

    @Test
    fun `GIVEN no mask WHEN filter THEN digits get a persistent plus prefix`() {
        // Act
        val result = transformation(mask = "").filter(AnnotatedString("2345"))

        // Assert
        assertThat(result.text.text).isEqualTo("+2345")
        assertThat(result.text.spanStyles).containsExactly(
            AnnotatedString.Range(SpanStyle(color = enteredColor), 0, 5),
        )
        assertThat(result.offsetMapping.originalToTransformed(4)).isEqualTo(5)
        assertThat(result.offsetMapping.transformedToOriginal(4)).isEqualTo(3)
    }

    @Test
    fun `GIVEN no mask and no input WHEN filter THEN only the plus is shown and the caret sits after it`() {
        // Act
        val result = transformation(mask = "").filter(AnnotatedString(""))

        // Assert
        assertThat(result.text.text).isEqualTo("+")
        assertThat(result.offsetMapping.originalToTransformed(0)).isEqualTo(1)
        assertThat(result.offsetMapping.transformedToOriginal(0)).isEqualTo(0)
    }

    @ParameterizedTest
    @MethodSource("boundsCases")
    fun masklessOffsetsStayWithinBounds(raw: String) {
        // Arrange
        val transformed = transformation(mask = "").filter(AnnotatedString(raw))
        val mapping = transformed.offsetMapping

        // Act & Assert
        for (offset in 0..raw.length) {
            assertThat(mapping.originalToTransformed(offset)).isIn(0..transformed.text.text.length)
        }
        for (offset in 0..transformed.text.text.length) {
            assertThat(mapping.transformedToOriginal(offset)).isIn(0..raw.length)
        }
    }

    @ParameterizedTest
    @MethodSource("originalToTransformedCases")
    fun originalToTransformed(model: OffsetModel) {
        // Arrange
        val mapping = transformation().filter(AnnotatedString(model.raw)).offsetMapping

        // Act
        val actual = mapping.originalToTransformed(model.offset)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("transformedToOriginalCases")
    fun transformedToOriginal(model: OffsetModel) {
        // Arrange
        val mapping = transformation().filter(AnnotatedString(model.raw)).offsetMapping

        // Act
        val actual = mapping.transformedToOriginal(model.offset)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("boundsCases")
    fun offsetsStayWithinBounds(raw: String) {
        // Arrange
        val transformed = transformation().filter(AnnotatedString(raw))
        val mapping = transformed.offsetMapping

        // Act & Assert
        for (offset in 0..raw.length) {
            assertThat(mapping.originalToTransformed(offset)).isIn(0..transformed.text.text.length)
        }
        for (offset in 0..transformed.text.text.length) {
            assertThat(mapping.transformedToOriginal(offset)).isIn(0..raw.length)
        }
    }

    internal data class OffsetModel(val raw: String, val offset: Int, val expected: Int)
    internal data class MaskedTextModel(val raw: String, val expected: String)
    internal data class SpanStyleModel(val raw: String, val expected: List<AnnotatedString.Range<SpanStyle>>)

    private fun maskedTextCases() = listOf(
        MaskedTextModel(raw = "", expected = "+1 (___) ___-____"),
        MaskedTextModel(raw = "234", expected = "+1 (234) ___-____"),
        MaskedTextModel(raw = "2345678901", expected = "+1 (234) 567-8901"),
    )

    private fun spanStyleCases() = listOf(
        SpanStyleModel(
            raw = "",
            expected = listOf(
                AnnotatedString.Range(SpanStyle(color = enteredColor), 0, 3),
                AnnotatedString.Range(SpanStyle(color = hintColor), 3, 17),
            ),
        ),
        SpanStyleModel(
            raw = "234",
            expected = listOf(
                AnnotatedString.Range(SpanStyle(color = enteredColor), 0, 9),
                AnnotatedString.Range(SpanStyle(color = hintColor), 9, 17),
            ),
        ),
        SpanStyleModel(
            raw = "2345678901",
            expected = listOf(AnnotatedString.Range(SpanStyle(color = enteredColor), 0, 17)),
        ),
    )

    private fun originalToTransformedCases() = listOf(
        OffsetModel(raw = "", offset = 0, expected = 4),
        OffsetModel(raw = "2345678901", offset = 0, expected = 4),
        OffsetModel(raw = "2345678901", offset = 1, expected = 5),
        OffsetModel(raw = "2345678901", offset = 3, expected = 7),
        OffsetModel(raw = "2345678901", offset = 10, expected = 17),
        OffsetModel(raw = "234", offset = 3, expected = 7),
    )

    private fun transformedToOriginalCases() = listOf(
        OffsetModel(raw = "2345678901", offset = 0, expected = 0),
        OffsetModel(raw = "2345678901", offset = 4, expected = 0),
        OffsetModel(raw = "2345678901", offset = 5, expected = 1),
        OffsetModel(raw = "2345678901", offset = 17, expected = 10),
        OffsetModel(raw = "234", offset = 100, expected = 3),
        OffsetModel(raw = "2345678901", offset = -5, expected = 0),
    )

    private fun boundsCases() = listOf("", "2", "234", "2345678901")
}