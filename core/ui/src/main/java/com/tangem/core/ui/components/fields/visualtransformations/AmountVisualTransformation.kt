package com.tangem.core.ui.components.fields.visualtransformations

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CURRENCY_SPACE
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.utils.defaultFormat
import com.tangem.core.ui.utils.formatWithThousands
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class AmountVisualTransformation(
    private val decimals: Int,
    private val symbol: String? = null,
    private val currencyCode: String? = null,
    private val decimalFormat: DecimalFormat = DecimalFormat(),
    private val symbolColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        var formattedAmount = decimalFormat.formatWithThousands(
            text.text,
            decimals,
        )
        formattedAmount = formattedAmount.ifEmpty { decimalFormat.defaultFormat() }
        val formattedText = if (formattedAmount.isNotEmpty() && symbol != null) {
            buildAnnotatedString {
                if (currencyCode != null) {
                    append(
                        formatFiatEditableAmount(
                            fiatAmount = formattedAmount,
                            fiatCurrencyCode = currencyCode,
                            fiatCurrencySymbol = symbol,
                            fiatCurrencySymbolColor = symbolColor,
                        ),
                    )
                } else {
                    append(formattedAmount)
                    append(CURRENCY_SPACE)
                    appendColored(symbol, symbolColor)
                }
            }
        } else {
            AnnotatedString(decimalFormat.defaultFormat())
        }

        val groupingSymbol = decimalFormat.decimalFormatSymbols.groupingSeparator
        return TransformedText(
            text = formattedText,
            offsetMapping = OffsetMappingImpl(text.text, formattedText, symbol, groupingSymbol),
        )
    }

    private fun formatFiatEditableAmount(
        fiatAmount: String?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        fiatCurrencySymbolColor: Color,
        locale: Locale = Locale.getDefault(),
    ): AnnotatedString {
        if (fiatAmount == null) return AnnotatedString(BigDecimalFormatConstants.EMPTY_BALANCE_SIGN)

        val formatterCurrency = getJavaCurrencyByCode(fiatCurrencyCode)
        val numberFormatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = formatterCurrency
        }
        val formatter = requireNotNull(numberFormatter as? DecimalFormat) {
            Timber.e("NumberFormat is null")
            return AnnotatedString(BigDecimalFormatConstants.EMPTY_BALANCE_SIGN)
        }
        return buildAnnotatedString {
            appendColored(
                formatter.positivePrefix.replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol),
                fiatCurrencySymbolColor,
            )
            append(fiatAmount)
            appendColored(
                formatter.positiveSuffix.replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol),
                fiatCurrencySymbolColor,
            )
        }
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