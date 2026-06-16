package com.tangem.core.ui.components.fields.visualtransformations

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CURRENCY_SPACE
import com.tangem.core.ui.utils.defaultFormat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AmountVisualTransformationTest {

    // Fixed US-style symbols so grouping (',') / decimal ('.') separators are deterministic across machines.
    private fun usFormat(): DecimalFormat = DecimalFormat().apply {
        decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
    }

    @Test
    fun `GIVEN symbol and no currency code WHEN filter THEN amount is followed by the symbol`() {
        // Arrange
        val sut = AmountVisualTransformation(decimals = 2, symbol = "ETH", decimalFormat = usFormat())

        // Act
        val result = sut.filter(AnnotatedString("1234.5"))

        // Assert — "1,234.5" + non-breaking space + "ETH"
        assertThat(result.text.text).isEqualTo("1,234.5${CURRENCY_SPACE}ETH")
    }

    @Test
    fun `GIVEN no symbol and no currency code WHEN filter THEN only the formatted amount is shown`() {
        // Arrange
        val sut = AmountVisualTransformation(decimals = 2, decimalFormat = usFormat())

        // Act
        val result = sut.filter(AnnotatedString("1234.5"))

        // Assert
        assertThat(result.text.text).isEqualTo("1,234.5")
    }

    @Test
    fun `GIVEN empty input WHEN filter THEN default formatted value is shown with symbol`() {
        // Arrange
        val format = usFormat()
        val sut = AmountVisualTransformation(decimals = 2, symbol = "ETH", decimalFormat = format)

        // Act
        val result = sut.filter(AnnotatedString(""))

        // Assert — empty input collapses to the default format, then the symbol branch appends the symbol
        assertThat(result.text.text).isEqualTo("${format.defaultFormat()}${CURRENCY_SPACE}ETH")
    }

    @Test
    fun `GIVEN grouping separators WHEN transformedToOriginal THEN separators are subtracted from offset`() {
        // Arrange
        val sut = AmountVisualTransformation(decimals = 2, symbol = "ETH", decimalFormat = usFormat())
        val result = sut.filter(AnnotatedString("1234"))
        // transformed text is "1,234 ETH" (one grouping separator before offset 5)

        // Act — caret at the end of the digits in transformed space ("1,234" -> index 5)
        val original = result.offsetMapping.transformedToOriginal(5)

        // Assert — minus the one grouping separator => 4 original digits
        assertThat(original).isEqualTo(4)
    }

    @Test
    fun `GIVEN grouping separators WHEN originalToTransformed THEN offset accounts for inserted separators`() {
        // Arrange
        val sut = AmountVisualTransformation(decimals = 2, symbol = "ETH", decimalFormat = usFormat())
        val result = sut.filter(AnnotatedString("1234"))

        // Act — original caret after all 4 digits
        val transformed = result.offsetMapping.originalToTransformed(4)

        // Assert — coerced to just before the currency symbol; 4 digits + 1 separator = 5
        assertThat(transformed).isEqualTo(5)
    }
}