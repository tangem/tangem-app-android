package com.tangem.core.ui.components.fields.visualtransformations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.defaultFormat
import com.tangem.core.ui.utils.formatWithThousands
import com.tangem.core.ui.utils.parseToBigDecimal
import java.text.DecimalFormat

class AmountVisualTransformation(
    private val decimals: Int,
    private val symbol: String? = null,
    private val currencyCode: String? = null,
    private val decimalFormat: DecimalFormat = DecimalFormat(),
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        var formattedAmount = decimalFormat.formatWithThousands(
            text.text,
            decimals,
        )
        formattedAmount = formattedAmount.ifEmpty { decimalFormat.defaultFormat() }
        val decimalValue = text.text.parseToBigDecimal(decimals)
        val formattedText = if (formattedAmount.isNotEmpty() && symbol != null) {
            AnnotatedString(
                if (currencyCode != null) {
                    BigDecimalFormatter.formatFiatAmount(
                        fiatAmount = decimalValue,
                        fiatCurrencyCode = currencyCode,
                        fiatCurrencySymbol = symbol,
                    )
                } else {
                    BigDecimalFormatter.formatCryptoAmountUncapped(
                        cryptoAmount = decimalValue,
                        cryptoSymbol = symbol,
                        decimals = decimals,
                    )
                },
            )
        } else {
            AnnotatedString(decimalFormat.defaultFormat())
        }

        val groupingSymbol = decimalFormat.decimalFormatSymbols.groupingSeparator
        return TransformedText(
            text = formattedText,
            offsetMapping = OffsetMappingImpl(text.text, formattedText, symbol, groupingSymbol),
        )
    }

    private class OffsetMappingImpl(
        private val text: String,
        private val formattedText: AnnotatedString,
        private val currencySymbol: String?,
        private val gropingSymbol: Char,
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var noneDigitCount = 0
            var i = 0
            val symbolOffset = currencySymbol?.let { formattedText.indexOf(it) } ?: -1
            while (i < offset + noneDigitCount) {
                val char = formattedText.getOrNull(i++)
                if (char == gropingSymbol) noneDigitCount++
                if (symbolOffset == 0 && char?.isWhitespace() == true) noneDigitCount++
            }
            var transformedOffset = if (symbolOffset == 0 && currencySymbol != null) {
                currencySymbol.length + offset + noneDigitCount
            } else {
                offset + noneDigitCount
            }
            transformedOffset = if (symbolOffset > 0) {
                transformedOffset.coerceIn(0, minOf(symbolOffset, formattedText.length))
            } else {
                transformedOffset.coerceIn(0, formattedText.length)
            }
            return transformedOffset
        }

        override fun transformedToOriginal(offset: Int): Int {
            val noneDigitCount = formattedText.take(offset).count { it == gropingSymbol }
            return (offset - noneDigitCount).coerceIn(0, text.length)
        }
    }
}