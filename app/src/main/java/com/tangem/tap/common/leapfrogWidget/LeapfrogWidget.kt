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
    private val foldAnimationDuration = 500L
    private val leapAnimationDuration = 800L
    private val leapBackAnimationDuration = 800L

    private val leapViews = mutableListOf<LeapView>()

    private var foldUnfoldProgress: Int = 100
    private var leapProgress: Int = 100
    private var leapBackProgress: Int = 100

    private var isFolded: Boolean = true

    init {
        calculator.setTranslationConverter { parentContainer.dpToPx(it) }
        init()
    }

    private fun init() {
        if (!viewIsFullFledged()) return

        val maxPosition = parentContainer.childCount - 1
        parentContainer.children.forEachIndexed { index, view ->
            val initialPosition = maxPosition - index
            leapViews.add(LeapView(view, index, initialPosition, maxPosition, calculator))
        }
    }

    fun fold(listener: ProgressListener? = null) {
        if (!canFoldUnfold() || isFolded) return

        createFoldUnfoldAnimation(true, foldAnimationDuration, listener)
    }

    fun unfold(listener: ProgressListener? = null) {
        if (!canFoldUnfold() || !isFolded) return

        createFoldUnfoldAnimation(false, foldAnimationDuration, listener)
    }

    private fun createFoldUnfoldAnimation(isFoldAnimation: Boolean, duration: Long, listener: ProgressListener?) {
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            if (isFoldAnimation) {
                it.fold()
                animatorsList.add(it.view.foldAnimation(duration, it.currentProperties))
            } else {
                it.unfold()
                animatorsList.add(it.view.unfoldAnimation(duration, it.currentProperties))
            }
        }
        animatorsList.add(createProgressListener(duration) {
            foldUnfoldProgress = it
            if (it == 100) isFolded = isFoldAnimation
        })
        animatorsList.add(createProgressListener(duration, listener))

        val animator = AnimatorSet()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.playTogether(animatorsList.toList())
        animator.start()
    }

    fun leap(listener: ProgressListener? = null) {
        if (!canLeap()) return

        val duration = leapAnimationDuration
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            when (it.leap()) {
                LeapFrogAnimation.LEAP -> {
                    val overLift = calculator.overLift(leapViews.size)
                    animatorsList.add(it.view.leapAnimation(duration, it.currentProperties, overLift))
                }
                LeapFrogAnimation.PULL -> {
                    animatorsList.add(it.view.pullUpAnimation(duration, it.currentProperties))
                }
            }
        }
        animatorsList.add(createProgressListener(duration) { leapProgress = it })
        animatorsList.add(createProgressListener(duration, listener))

        val animator = AnimatorSet()
        animator.playTogether(animatorsList.toList())
        animator.start()
    }

    fun leapBack(listener: ProgressListener? = null) {
        if (!canLeapBack()) return

        val duration = leapBackAnimationDuration
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            when (it.leapBack()) {
                LeapFrogAnimation.LEAP -> {
                    animatorsList.add(it.view.leapBackAnimation(duration, it.currentProperties, calculator))
                }
                LeapFrogAnimation.PULL -> {
                    animatorsList.add(it.view.pullDownAnimation(duration, it.currentProperties))
                }
            }
        }
        animatorsList.add(createProgressListener(duration) { leapBackProgress = it })
        animatorsList.add(createProgressListener(duration, listener))
        val animator = AnimatorSet()
        animator.playTogether(animatorsList.toList())
        animator.start()
    }

    fun getViewByPosition(position: Int): View? {
        return leapViews.firstOrNull { it.currentPosition == position }?.view
    }

    private fun viewIsFullFledged(): Boolean = parentContainer.childCount > 1

    private fun canFoldUnfold(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            foldUnfoldInProgress() -> false
            leapInProgress() || leapBackInProgress() -> false
            else -> true
        }
    }

    private fun canLeap(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            isFolded -> false
            leapBackInProgress() -> false
            leapProgress < 70 -> false
            else -> true
        }
    }

    private fun canLeapBack(): Boolean {
        return when {
            !viewIsFullFledged() -> false
            isFolded -> false
            leapInProgress() -> false
            leapBackProgress < 70 -> false
            else -> true
        }
    }

    private fun foldUnfoldInProgress(): Boolean = foldUnfoldProgress != 100
    private fun leapInProgress(): Boolean = leapProgress != 100
    private fun leapBackInProgress(): Boolean = leapBackProgress != 100
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