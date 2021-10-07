package com.tangem.tap.common.leapfrogWidget

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.children
import com.tangem.tangem_sdk_new.extensions.dpToPx

/**
[REDACTED_AUTHOR]
 */
class LeapfrogWidget(
    private val parentContainer: FrameLayout,
    private val calculator: PropertyCalculator = PropertyCalculator(),
) {
    private val initAnimationDuration = 500L
    private val leapAnimationDuration = 800L
    private val leapBackAnimationDuration = 800L

    private val leapViews = mutableListOf<LeapView>()

    private var leapProgress: Int = 100
    private var leapBackProgress: Int = 100

    init {
        calculator.setTranslationConverter { parentContainer.dpToPx(it) }
        init()
    }

    private fun init() {
        if (!viewIsFullFledged()) return

        val maxPosition = parentContainer.childCount - 1
        parentContainer.children.forEachIndexed { index, view ->
            val initialPosition = maxPosition - index
            val initialProperties = createViewProperty(0, initialPosition)
            leapViews.add(LeapView(view, index, initialPosition, maxPosition, initialProperties))
        }
        resetToInitialState()
    }

    private fun resetToInitialState(withAnimation: Boolean = true, duration: Long = initAnimationDuration) {
        if (!viewIsFullFledged()) return

        if (withAnimation) {
            val animatorsList = mutableListOf<AnimatorSet>()
            leapViews.forEach { animatorsList.add(it.view.initAnimation(duration, it.initialProperties)) }
            val animator = AnimatorSet()
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.playTogether(animatorsList.toList())
            animator.start()
        } else {
            leapViews.forEach {
                it.view.scaleX = it.initialProperties.scaleEnd
                it.view.scaleY = it.initialProperties.scaleEnd
                it.view.translationY = it.initialProperties.yTranslationEnd
                it.view.elevation = it.initialProperties.elevationEnd
            }
        }
    }

    fun leap(listener: ProgressListener? = null) {
        if (!canLeap()) return

        // if not - then it's ready for pullUp
        fun isReadyForLeap(leapView: LeapView): Boolean {
            return leapView.previousPosition == 0 && leapView.currentPosition == leapView.maximalPosition
        }

        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            it.changePositionByLeap()
            val properties = createViewProperty(it.previousPosition, it.currentPosition)
            if (isReadyForLeap(it)) {
                val overLift = calculator.overLift(leapViews.size)
                animatorsList.add(it.view.leapAnimation(leapAnimationDuration, properties, overLift))
            } else {
                animatorsList.add(it.view.pullUpAnimation(leapAnimationDuration, properties))
            }
        }
        val animator = AnimatorSet()
        animatorsList.add(animator.createProgressListener(leapAnimationDuration) { leapProgress = it })
        animatorsList.add(animator.createProgressListener(leapAnimationDuration, listener))
        animator.playTogether(animatorsList.toList())
        animator.start()
    }

    fun leapBack(listener: ProgressListener? = null) {
        if (!canLeapBack()) return

        // if not - then it's ready for pullDown
        fun isReadyForLeapBack(leapView: LeapView): Boolean {
            return leapView.previousPosition == leapView.maximalPosition && leapView.currentPosition == 0
        }

        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            it.changePositionByLeapBack()
            val properties = createViewProperty(it.previousPosition, it.currentPosition)
            if (isReadyForLeapBack(it)) {
                val overLift = calculator.overLift(leapViews.size)
                animatorsList.add(it.view.leapBackAnimation(leapBackAnimationDuration, properties, calculator))
            } else {
                animatorsList.add(it.view.pullDownAnimation(leapBackAnimationDuration, properties))
            }
        }
        val animator = AnimatorSet()
        animatorsList.add(animator.createProgressListener(leapBackAnimationDuration) { leapBackProgress = it })
        animatorsList.add(animator.createProgressListener(leapBackAnimationDuration, listener))
        animator.playTogether(animatorsList.toList())
        animator.start()
    }

    fun getViewByPosition(position: Int): View? {
        return leapViews.firstOrNull { it.currentPosition == position }?.view
    }

    private fun viewIsFullFledged(): Boolean = parentContainer.childCount > 1

    private fun canLeap(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            leapBackInProgress() -> false
            leapProgress < 70 -> false
            else -> true
        }
    }

    private fun canLeapBack(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            leapInProgress() -> false
            leapBackProgress < 70 -> false
            else -> true
        }
    }

    private fun leapInProgress(): Boolean = leapProgress != 100
    private fun leapBackInProgress(): Boolean = leapBackProgress != 100

    private fun createViewProperty(startPosition: Int, endPosition: Int): Properties {
        return Properties(
                startPosition,
                endPosition,
                calculator.scale(startPosition),
                calculator.scale(endPosition),
                calculator.elevation(startPosition, parentContainer.childCount),
                calculator.elevation(endPosition, parentContainer.childCount),
                calculator.yTranslation(startPosition),
                calculator.yTranslation(endPosition),
                calculator.hasForeground(startPosition),
                calculator.hasForeground(endPosition),
        )
    }
}

class PropertyCalculator(
    val elevationFactor: Float = 1f,
    val decreaseScaleFactor: Float = 0.1f,
    val yTranslationFactor: Float = 20f,
    val decreaseOverLiftingFactor: Float = 0.85f,
) {
    // 1st view it is view on the top of stack, but it is last in parent.children

    private var translationValueConverter: ((Float) -> Float)? = null
    fun setTranslationConverter(converter: (Float) -> Float) {
        translationValueConverter = converter
    }

    fun scale(position: Int): Float {
        return 1f - decreaseScaleFactor * position
    }

    fun elevation(position: Int, count: Int): Float {
        return elevationFactor * (count - 1 - position)
    }

    fun yTranslation(position: Int): Float {
        val yTranslation = yTranslationFactor * position
        return convertTranslation(yTranslation)
    }

    fun hasForeground(position: Int): Boolean {
        return position != 0
    }

    fun overLift(viewCount: Int): Float {
        val initialOverLift = when {
            yTranslationFactor < 0f -> yTranslationFactor * decreaseOverLiftingFactor * viewCount * -1
            else -> 10f
        }
        val scaleOverLift = when {
            decreaseScaleFactor == 0f -> initialOverLift
            else -> initialOverLift * decreaseOverLiftingFactor - initialOverLift * decreaseScaleFactor
        }
        return convertTranslation(scaleOverLift)
    }

    private fun convertTranslation(value: Float): Float {
        return translationValueConverter?.invoke(value) ?: value
    }
}