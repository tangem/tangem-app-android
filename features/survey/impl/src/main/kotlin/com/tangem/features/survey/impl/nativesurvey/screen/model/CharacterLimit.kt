package com.tangem.features.survey.impl.nativesurvey.screen.model

import java.text.BreakIterator

/** Takes at most [maxLength] UTF-16 units, cut at a character boundary so an emoji or an accent is never split. */
internal fun String.takeWholeCharacters(maxLength: Int): String {
    if (length <= maxLength) return this

    val boundaries = BreakIterator.getCharacterInstance().apply { setText(this@takeWholeCharacters) }
    return substring(0, boundaries.preceding(maxLength + 1))
}