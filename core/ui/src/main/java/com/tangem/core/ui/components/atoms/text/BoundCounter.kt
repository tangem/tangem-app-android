package com.tangem.core.ui.components.atoms.text

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult

internal class BoundCounter(
    private val text: String,
    private val textLayoutResult: TextLayoutResult,
    private val charPosition: (Int) -> Int,
) {
    var string = ""
        private set
    var width = 0f
        private set

    private var _nextCharWidth: Float? = null
    private var invalidCharsCount = 0

    fun widthWithNextChar(): Float = width + nextCharWidth()

    private fun nextCharWidth(): Float = _nextCharWidth ?: run {
        var boundingBox: Rect
        // invalidCharsCount fixes this bug: https://issuetracker.google.com/issues/197146630
        invalidCharsCount--
        do {
            boundingBox = textLayoutResult
                .getBoundingBox(charPosition(string.count() + ++invalidCharsCount))
        } while (boundingBox.right == 0f)
        _nextCharWidth = boundingBox.width
        boundingBox.width
    }

    fun addNextChar() {
        string += text[charPosition(string.count())]
        width += nextCharWidth()
        _nextCharWidth = null
    }
}