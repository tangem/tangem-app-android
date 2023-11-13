package com.tangem.core.ui.components.fields

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class AmountVisualTransformation(
    private val symbol: String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            buildAnnotatedString {
                append(text)
                if (text.isNotBlank()) {
                    append(" ")
                    append(symbol)
                }
            },
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return text.length
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return text.length
                }
            },
        )
    }
}
