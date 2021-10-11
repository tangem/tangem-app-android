package com.tangem.tap.common.leapfrogWidget

import android.view.View

/**
[REDACTED_AUTHOR]
 */
class LeapView(
    val view: View,
    val index: Int,
    position: Int,
    private val maximalPosition: Int,
    private val calculator: PropertyCalculator,
) {
    var currentProperties: AnimationProperties
        private set

    var currentPosition: Int = position
        private set
    var previousPosition: Int = position
        private set

    init {
        val initialProperties = createInitialProperty(position)
        currentProperties = AnimationProperties.from(initialProperties, initialProperties)

        view.elevation = initialProperties.elevation
        view.translationY = initialProperties.yTranslation
        view.scaleX = initialProperties.scale
        view.scaleY = initialProperties.scale
//        initialProperties.hasForeground
    }

    fun leap(): LeapFrogAnimation {
        previousPosition = currentPosition
        currentPosition = when (previousPosition) {
            0 -> maximalPosition
            else -> currentPosition - 1
        }
        updateProperties(createLeapAnimationProperty(previousPosition, currentPosition))

        return when {
            previousPosition == 0 && currentPosition == maximalPosition -> LeapFrogAnimation.LEAP
            else -> LeapFrogAnimation.PULL
        }
    }

    fun leapBack(): LeapFrogAnimation {
        previousPosition = currentPosition
        currentPosition = when (previousPosition) {
            maximalPosition -> 0
            else -> currentPosition + 1
        }
        updateProperties(createLeapAnimationProperty(previousPosition, currentPosition))

        return when {
            previousPosition == maximalPosition && currentPosition == 0 -> LeapFrogAnimation.LEAP
            else -> LeapFrogAnimation.PULL
        }
    }

    fun fold() {
        updateProperties(currentProperties.toFold())
    }

    fun unfold() {
        updateProperties(currentProperties.toUnfold(calculator))
    }

    private fun updateProperties(properties: AnimationProperties) {
        currentProperties = properties
    }

    private fun createInitialProperty(endPosition: Int): Properties {
        return Properties(
                endPosition,
                calculator.scale(endPosition),
                calculator.elevation(endPosition, maximalPosition + 1),
                calculator.yTranslation(0),
                calculator.hasForeground(endPosition),
        )
    }

    private fun createLeapAnimationProperty(startPosition: Int, endPosition: Int): AnimationProperties {
        return AnimationProperties(
                startPosition,
                endPosition,
                calculator.scale(startPosition),
                calculator.scale(endPosition),
                calculator.elevation(startPosition, maximalPosition + 1),
                calculator.elevation(endPosition, maximalPosition + 1),
                calculator.yTranslation(startPosition),
                calculator.yTranslation(endPosition),
                calculator.hasForeground(startPosition),
                calculator.hasForeground(endPosition),
        )
    }

    private fun AnimationProperties.toUnfold(calculator: PropertyCalculator): AnimationProperties {
        val start = this.yTranslationEnd
        val end = calculator.yTranslation(this.positionEnd)
        return this.copy(yTranslationStart = start, yTranslationEnd = end)
    }

    private fun AnimationProperties.toFold(): AnimationProperties {
        val start = this.yTranslationEnd
        val end = 0f
        return this.copy(yTranslationStart = start, yTranslationEnd = end)
    }

    override fun toString(): String = "index: $index, position: $currentPosition, code: ${view.hashCode()}"

}

enum class LeapFrogAnimation {
    LEAP, PULL
}

data class Properties(
    val position: Int,
    val scale: Float,
    val elevation: Float,
    val yTranslation: Float,
    val hasForeground: Boolean,
)

data class AnimationProperties(
    val positionStart: Int,
    val positionEnd: Int,
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

    companion object {
        fun from(start: Properties, end: Properties): AnimationProperties {
            return AnimationProperties(
                    start.position, end.position,
                    start.scale, end.scale,
                    start.elevation, end.elevation,
                    start.yTranslation, end.yTranslation,
                    start.hasForeground, end.hasForeground
            )
        }
    }
}