package com.tangem.core.ui.components.fields.visualtransformations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.core.ui.utils.formatWithThousands
import java.text.DecimalFormat

class AmountVisualTransformation(
    private val decimals: Int,
    private val symbol: String? = null,
    private val decimalFormat: DecimalFormat = DecimalFormat(),
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val formattedText = decimalFormat.formatWithThousands(
            text.text,
            decimals,
        )
        val groupingSymbol = decimalFormat.decimalFormatSymbols.groupingSeparator
        return TransformedText(
            text = buildAnnotatedString {
                append(formattedText)
                if (formattedText.isNotEmpty() && symbol != null) {
                    append(" $symbol")
                }
            },
            offsetMapping = OffsetMappingImpl(text.text, formattedText, groupingSymbol),
        )
    }

    private class OffsetMappingImpl(
        private val text: String,
        private val formattedText: String,
        private val gropingSymbol: Char,
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var noneDigitCount = 0
            var i = 0
            while (i < offset + noneDigitCount) {
                if (formattedText.getOrNull(i++) == gropingSymbol) noneDigitCount++
            }
            return (offset + noneDigitCount).coerceIn(0, formattedText.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
            val noneDigitCount = formattedText.take(offset).count { it == gropingSymbol }
            return (offset - noneDigitCount).coerceIn(0, text.length)
        }
    }
}