package com.tangem.features.hotwallet.addexistingwallet.im.port.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.tangem.utils.StringsSigns.WHITE_SPACE
import com.tangem.utils.extensions.isSingleItem
import kotlinx.collections.immutable.ImmutableList

class InvalidWordsColorTransformation(
    private val wordsToBrush: ImmutableList<String>,
    private val style: SpanStyle,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val plainText = text.text
        if (wordsToBrush.isSingleItem() && wordsToBrush.first() == plainText) {
            return plainText.annotate().toTransformedText()
        }

        val wordList = makeSeparatedWordlist(plainText)
        val annotatedString = makeAnnotatedString(wordList)

        return annotatedString.toTransformedText()
    }

    private fun makeSeparatedWordlist(plainText: String): List<String> {
        val plainTextCharArray = plainText.toCharArray()
        val wordlist = mutableListOf<String>()
        var wordBuilder = StringBuilder()

        for (index in plainTextCharArray.indices) {
            val char = plainTextCharArray[index]
            if (char.isWhitespace()) {
                if (wordBuilder.isEmpty()) {
                    wordlist.add(WORD_SEPARATOR)
                } else {
                    wordlist.add(wordBuilder.toString())
                    wordBuilder = StringBuilder()
                    wordlist.add(WORD_SEPARATOR)
                }
            } else {
                if (index == plainTextCharArray.lastIndex) {
                    wordBuilder.append(char)
                    wordlist.add(wordBuilder.toString())
                    wordBuilder = StringBuilder()
                } else {
                    wordBuilder.append(char)
                }
            }
        }
        return wordlist.toList()
    }

    private fun makeAnnotatedString(wordList: List<String>): AnnotatedString = buildAnnotatedString {
        wordList.forEach {
            if (it == WORD_SEPARATOR) {
                append(WHITE_SPACE)
            } else if (wordsToBrush.contains(it)) {
                append(it.annotate())
            } else {
                append(it)
            }
        }
    }

    private fun String.annotate(): AnnotatedString = AnnotatedString(this, style)

    private fun AnnotatedString.toTransformedText(): TransformedText = TransformedText(this, OffsetMapping.Identity)

    companion object {
        private const val WORD_SEPARATOR = "_$@-*"
    }
}