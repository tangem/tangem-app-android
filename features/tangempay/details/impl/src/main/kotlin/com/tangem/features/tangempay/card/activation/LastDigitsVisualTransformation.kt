package com.tangem.features.tangempay.card.activation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.tangem.domain.pay.model.CARD_ACTIVATION_LAST_DIGITS_LENGTH

internal const val MASK_DIGIT = "0"

internal data class LastDigitsVisualTransformation(
    private val enteredColor: Color,
    private val maskColor: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val entered = text.text.take(CARD_ACTIVATION_LAST_DIGITS_LENGTH)
        val transformed = buildAnnotatedString {
            withStyle(SpanStyle(color = enteredColor)) { append(entered) }
            withStyle(SpanStyle(color = maskColor)) {
                append(MASK_DIGIT.repeat(CARD_ACTIVATION_LAST_DIGITS_LENGTH - entered.length))
            }
        }
        return TransformedText(text = transformed, offsetMapping = MaskPaddingOffsetMapping(entered.length))
    }

    private class MaskPaddingOffsetMapping(private val enteredLength: Int) : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, CARD_ACTIVATION_LAST_DIGITS_LENGTH)

        override fun transformedToOriginal(offset: Int): Int = offset.coerceIn(0, enteredLength)
    }
}