package com.tangem.tap.common.leapfrogWidget

import android.view.View

/**
[REDACTED_AUTHOR]
 */
class LeapView(
    val view: View,
    val index: Int,
    position: Int,
    val maximalPosition: Int,
    val initialProperties: Properties
) {
    var currentPosition: Int = position
        private set
    var previousPosition: Int = position
        private set

    fun changePositionByLeap() {
        previousPosition = currentPosition
        currentPosition = when (previousPosition) {
            0 -> maximalPosition
            else -> currentPosition - 1
        }
    }

    fun changePositionByLeapBack() {
        previousPosition = currentPosition
        currentPosition = when (previousPosition) {
            maximalPosition -> 0
            else -> currentPosition + 1
        }
    }

    override fun toString(): String = "index: $index, position: $currentPosition, code: ${view.hashCode()}"
}

data class Properties(
    val startPosition: Int,
    val endPosition: Int,
    val scaleStart: Float,
    val scaleEnd: Float,
    val elevationStart: Float,
    val elevationEnd: Float,
    val yTranslationStart: Float,
    val yTranslationEnd: Float,
    val hasForegroundStart: Boolean,
    val hasForegroundEnd: Boolean,
) {
    override fun toString(): String {
        return """
            scaleStart: $scaleStart, scaleEnd: $scaleEnd, 
            elevationStart: $elevationStart, elevationEnd: $elevationEnd, 
            yTranslationStart: $yTranslationStart, yTranslationEnd: $yTranslationEnd, 
            hasForegroundStart : $hasForegroundStart, hasForegroundEnd: $hasForegroundEnd, 
        """.trimIndent()
    }
}