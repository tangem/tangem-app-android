package com.tangem.tap.common.leapfrogWidget

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
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

    var isFolded: Boolean = true
        private set

    init {
        calculator.setTranslationConverter { parentContainer.dpToPx(it) }
        if (viewIsFullFledged()) {
            val maxPosition = parentContainer.childCount - 1
            parentContainer.children.forEachIndexed { index, view ->
                val initialPosition = maxPosition - index
                leapViews.add(LeapView(view, index, initialPosition, maxPosition, calculator))
            }
        }
    }

    /**
     * Apply initial properties to the views
     */
    fun initViews() {
        isFolded = true
        leapViews.forEach { it.initView() }
    }

    fun fold(onEnd: () -> Unit = {}) {
        val animator = foldAnimator()
        if (animator == null) {
            onEnd()
        } else {
            animator.doOnEnd { onEnd() }
            animator.start()
        }
    }

    fun unfold(onEnd: () -> Unit = {}) {
        val animator = unfoldAnimator()
        if (animator == null) {
            onEnd()
        } else {
            animator.doOnEnd { onEnd() }
            animator.start()
        }
    }

    fun foldAnimator(): Animator? {
        if (!canFoldUnfold() || isFolded) return null

        return createFoldUnfoldAnimator(true, foldAnimationDuration)
    }

    fun unfoldAnimator(): Animator? {
        if (!canFoldUnfold() || !isFolded) return null

        return createFoldUnfoldAnimator(false, foldAnimationDuration)
    }

    private fun createFoldUnfoldAnimator(isFoldAnimation: Boolean, duration: Long): Animator {
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            if (isFoldAnimation) {
                it.fold()
                animatorsList.add(it.view.foldAnimation(duration, it.state.properties))
            } else {
                it.unfold()
                animatorsList.add(it.view.unfoldAnimation(duration, it.state.properties))
            }
        }
        animatorsList.add(createProgressListener(duration) {
            foldUnfoldProgress = it
            if (it == 100) isFolded = isFoldAnimation
        })

        val animator = AnimatorSet()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.playTogether(animatorsList.toList())
        return animator
    }

    fun leap(onEnd: () -> Unit = {}) {
        if (!canLeap()) {
            onEnd()
            return
        }

        val duration = leapAnimationDuration
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            when (it.leap()) {
                LeapFrogAnimation.LEAP -> {
                    val overLift = calculator.overLift(leapViews.size)
                    animatorsList.add(it.view.leapAnimation(duration, it.state.properties, overLift))
                }
                LeapFrogAnimation.PULL -> {
                    animatorsList.add(it.view.pullUpAnimation(duration, it.state.properties))
                }
            }
        }
        animatorsList.add(createProgressListener(duration) { leapProgress = it })

        val animator = AnimatorSet()
        animator.playTogether(animatorsList.toList())
        animator.doOnEnd { onEnd() }
        animator.start()
    }

    fun leapBack(onEnd: () -> Unit = {}) {
        if (!canLeapBack()) {
            onEnd()
            return
        }

        val duration = leapBackAnimationDuration
        val animatorsList = mutableListOf<Animator>()
        leapViews.forEach {
            when (it.leapBack()) {
                LeapFrogAnimation.LEAP -> {
                    animatorsList.add(it.view.leapBackAnimation(duration, it.state.properties, calculator))
                }
                LeapFrogAnimation.PULL -> {
                    animatorsList.add(it.view.pullDownAnimation(duration, it.state.properties))
                }
            }
        }
        animatorsList.add(createProgressListener(duration) { leapBackProgress = it })

        val animator = AnimatorSet()
        animator.playTogether(animatorsList.toList())
        animator.doOnEnd { onEnd() }
        animator.start()
    }

    fun getViewsCount(): Int = parentContainer.childCount

    fun getViewPositionByIndex(index: Int): Int = leapViews.first { it.index == index }.state.currentPosition

    fun getViewByPosition(position: Int): LeapView = leapViews.first { it.state.currentPosition == position }

    fun getState(): LeapfrogWidgetState = LeapfrogWidgetState(isFolded, leapViews.map { it.state })

    fun applyState(state: LeapfrogWidgetState) {
        isFolded = state.isFolded
        state.leapViewStates.forEach { state ->
            leapViews.firstOrNull { it.index == state.index }?.applyState(state)
        }
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

data class LeapfrogWidgetState(
    val isFolded: Boolean,
    val leapViewStates: List<LeapViewState>
)

class PropertyCalculator(
    val elevationFactor: Float = 1f,
    val decreaseScaleFactor: Float = 0.2f,
    val yTranslationFactor: Float = -40f,
    val decreaseOverLiftingFactor: Float = 0.85f,
) {
    // 0 view it is view on the top of stack, but it is last in parent.children

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