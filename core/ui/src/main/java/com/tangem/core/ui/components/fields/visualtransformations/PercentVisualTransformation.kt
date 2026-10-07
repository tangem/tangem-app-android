package com.tangem.core.ui.components.fields.visualtransformations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.utils.StringsSigns

/** Appends a `%` sign to the entered number and shows `0%` while the field is empty. */
object PercentVisualTransformation : VisualTransformation {

    private const val EMPTY_HINT = "0"

    override fun filter(text: AnnotatedString): TransformedText {
        val shown = text.text.ifEmpty { EMPTY_HINT }

        return TransformedText(
            text = buildAnnotatedString {
                append(shown)
                append(StringsSigns.PERCENT)
            },
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, shown.length)
                override fun transformedToOriginal(offset: Int): Int = offset.coerceIn(0, text.text.length)
            },
        )
    }
}