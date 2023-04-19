package com.tangem.feature.onboarding.domain

import com.tangem.utils.extensions.isNotWhitespace

/**
* [REDACTED_AUTHOR]
 * Class which provides helper functions to determine lead or last part of words for suggestions
 */
internal class PartWordFinder {

    /**
     * Searches and return part of a word on the left side of the [cursorPosition] into the [text]
     */
    fun getLeadPartOfWord(text: String, cursorPosition: Int): String? {
        val charText = text.toCharArray().asList()
        if (cursorPosition > charText.size || cursorPosition < 0) {
            // cursorPosition is out of text size
            return null
        }
        if (cursorPosition == 0) {
            // cursorPosition at start of text
            return null
        }
        if (charText[cursorPosition - 1].isWhitespace()) {
            // white space to the left of cursorPosition
            return null
        }
        if (charText.size > cursorPosition && charText[cursorPosition].isNotWhitespace()) {
            // cursorPosition in the middle of a word
            return null
        }

        for (charIndex in cursorPosition - 1 downTo 0) {
            val char = charText[charIndex]
            when {
                charIndex == 0 -> {
                    return text.substring(charIndex, cursorPosition)
                }
                char.isWhitespace() -> {
                    return text.substring(charIndex + 1, cursorPosition)
                }
            }
        }

        return null
    }

    /**
     * Returns a substring of the [word] which excludes the first word part found on the left of [cursorPosition] in
     * the [text]
     */
    fun getLastPartOfWord(text: String, word: String, cursorPosition: Int): String {
        val leadPart = getLeadPartOfWord(text, cursorPosition) ?: return ""

        return word.substring(leadPart.length, word.length)
    }
}

