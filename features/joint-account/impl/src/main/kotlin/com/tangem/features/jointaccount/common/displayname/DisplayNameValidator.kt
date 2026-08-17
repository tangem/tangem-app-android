package com.tangem.features.jointaccount.common.displayname

/**
 * Validates a member's display name. Stricter than the account name: 1–[MAX_LENGTH] characters; letters, digits,
 * spaces and emoji are allowed, special characters are not.
 *
 * The value is never normalised — no trimming, no case folding, no Unicode normalisation — because the name
 * becomes part of the payload signed by the card. Validation is a boolean gate only.
 *
 * The emoji check is a pragmatic approximation (pictographs plus the code points that assemble them into
 * sequences); the exact allowed set must be confirmed against the backend contract once it is finalised.
 */
internal class DisplayNameValidator {

    /** Spaces are allowed characters, but a name made only of them is still not a name */
    fun isValid(name: String): Boolean {
        return name.isNotBlank() && name.length <= MAX_LENGTH && !hasForbiddenChars(name)
    }

    fun hasForbiddenChars(name: String): Boolean {
        var index = 0
        while (index < name.length) {
            val codePoint = name.codePointAt(index)
            if (!isAllowedCodePoint(codePoint)) return true
            index += Character.charCount(codePoint)
        }
        return false
    }

    private fun isAllowedCodePoint(codePoint: Int): Boolean {
        return Character.isLetterOrDigit(codePoint) || codePoint == SPACE || isEmojiCodePoint(codePoint)
    }

    private fun isEmojiCodePoint(codePoint: Int): Boolean {
        return Character.getType(codePoint) == Character.OTHER_SYMBOL.toInt() ||
            codePoint == ZERO_WIDTH_JOINER ||
            codePoint == COMBINING_KEYCAP ||
            codePoint in VARIATION_SELECTORS ||
            codePoint in SKIN_TONE_MODIFIERS
    }

    companion object {

        const val MAX_LENGTH = 25

        private const val SPACE = ' '.code

        /** Joins pictographs into a single glyph, e.g. the family emoji */
        private const val ZERO_WIDTH_JOINER = 0x200D

        /** Turns a digit into a keycap emoji, e.g. 1️⃣ */
        private const val COMBINING_KEYCAP = 0x20E3

        /** VS15/VS16 select between the text and emoji presentation of the preceding character */
        private val VARIATION_SELECTORS = 0xFE00..0xFE0F

        /** Fitzpatrick modifiers; the rest of their `Sk` category is punctuation-like and stays forbidden */
        private val SKIN_TONE_MODIFIERS = 0x1F3FB..0x1F3FF
    }
}