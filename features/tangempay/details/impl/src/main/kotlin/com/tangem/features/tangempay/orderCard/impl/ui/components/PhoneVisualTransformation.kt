package com.tangem.features.tangempay.orderCard.impl.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.tangem.features.tangempay.orderCard.impl.model.PhoneMaskFormatter

internal class PhoneVisualTransformation(
    private val mask: String,
    private val enteredColor: Color,
    private val hintColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (mask.isEmpty()) return plusPrefixed(raw)
        val masked = PhoneMaskFormatter.applyWithHint(rawDigits = raw, mask = mask)
        val enteredLength = PhoneMaskFormatter.enteredLength(rawDigits = raw, mask = mask)
            .coerceIn(0, masked.length)
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = enteredColor)) {
                append(masked.substring(0, enteredLength))
            }
            if (enteredLength < masked.length) {
                withStyle(SpanStyle(color = hintColor)) {
                    append(masked.substring(enteredLength))
                }
            }
        }
        return TransformedText(annotated, MaskOffsetMapping(mask = mask, masked = masked, raw = raw))
    }

    private fun plusPrefixed(raw: String): TransformedText {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(color = enteredColor)) {
                append(PhoneMaskFormatter.PLUS)
                append(raw)
            }
        }
        return TransformedText(annotated, PlusOffsetMapping(rawLength = raw.length))
    }

    internal class PlusOffsetMapping(private val rawLength: Int) : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, rawLength) + 1

        override fun transformedToOriginal(offset: Int): Int = (offset - 1).coerceIn(0, rawLength)
    }

    internal class MaskOffsetMapping(
        private val mask: String,
        private val masked: String,
        private val raw: String,
    ) : OffsetMapping {

        private val firstDigitSlot = mask
            .indexOfFirst { it == PhoneMaskFormatter.PLACEHOLDER }
            .coerceAtLeast(0)
            .coerceAtMost(masked.length)

        override fun originalToTransformed(offset: Int): Int {
            if (offset <= 0) return firstDigitSlot
            var consumed = 0
            var transformedIndex = 0
            for (maskChar in mask) {
                if (consumed >= offset) break
                transformedIndex++
                if (maskChar == PhoneMaskFormatter.PLACEHOLDER) consumed++
            }
            return transformedIndex.coerceAtMost(masked.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
            val clamped = offset.coerceIn(0, masked.length)
            var consumed = 0
            for (index in 0 until clamped) {
                if (mask[index] == PhoneMaskFormatter.PLACEHOLDER) consumed++
            }
            return consumed.coerceAtMost(raw.length)
        }
    }
}