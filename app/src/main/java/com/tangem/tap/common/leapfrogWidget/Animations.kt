package com.tangem.tap.common.leapfrogWidget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
[REDACTED_AUTHOR]
 */
//fun View.setForeground(hasForegroundAtEnd: Boolean) {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//        foreground = getForegroundDrawable()
//    }
//}

//fun View.getForegroundDrawable(): Drawable? {
//    return AppCompatResources.getDrawable(context, R.drawable.shape_rectangle_rounded_8_op_20)?.also {
//        it.alpha = 60
//    }
//}

typealias ProgressListener = (Int) -> Unit

fun createProgressListener(duration: Long, listener: ProgressListener?): Animator {
    val valueAnimator = ValueAnimator.ofInt(0, 100)
    valueAnimator.duration = duration
    valueAnimator.addUpdateListener { listener?.invoke((it.animatedValue as? Int) ?: 0) }
    return valueAnimator
}

fun View.unfoldAnimation(animDuration: Long, properties: AnimationProperties): AnimatorSet {
    val translate = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslationStart, properties.yTranslationEnd)
    translate.duration = animDuration
    return AnimatorSet().apply { playTogether(translate) }
}

fun View.foldAnimation(animDuration: Long, properties: AnimationProperties): AnimatorSet {
    val translate = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslationStart, properties.yTranslationEnd)
    translate.duration = animDuration

    return AnimatorSet().apply {
        playSequentially(translate)
    }
}

fun View.leapAnimation(animDuration: Long, properties: AnimationProperties, overLift: Float): AnimatorSet {
    val halfDuration = animDuration / 2

    val upTo = (height.toFloat() + properties.yTranslationStart + overLift) * -1
    val translateUp = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslationStart, upTo)
    translateUp.duration = halfDuration
    translateUp.interpolator = LinearInterpolator()

    val scaleDuration = halfDuration - halfDuration / 5
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scaleStart, properties.scaleEnd)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scaleStart, properties.scaleEnd)
    val scale = AnimatorSet().apply {
        startDelay = scaleDuration
        duration = scaleDuration
        interpolator = LinearInterpolator()
        playTogether(scaleX, scaleY)
    }

    val elevation = ValueAnimator.ofFloat(properties.elevationStart, properties.elevationEnd)
    elevation.addUpdateListener { this.elevation = (it.animatedValue as? Float) ?: properties.elevationStart }
    elevation.startDelay = halfDuration
    elevation.duration = 100

    val translateDown = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, upTo, properties.yTranslationEnd)
    translateDown.startDelay = halfDuration
    translateDown.duration = halfDuration
    translateDown.interpolator = LinearInterpolator()

    return AnimatorSet().apply {
        playTogether(
                translateUp,
                scale,
                elevation,
                translateDown
        )
    }
}

fun View.leapBackAnimation(animDuration: Long, properties: AnimationProperties, calculator: PropertyCalculator): AnimatorSet {
    val halfDuration = animDuration / 2

    val upTo = ((height).toFloat() - calculator.yTranslationFactor) * -1
    val translateUp = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslationStart, upTo)
    translateUp.duration = halfDuration
    translateUp.interpolator = LinearInterpolator()

    val scaleDuration = halfDuration - halfDuration / 5
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scaleStart, properties.scaleEnd)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scaleStart, properties.scaleEnd)
    val scale = AnimatorSet().apply {
        startDelay = scaleDuration
        duration = scaleDuration
        interpolator = LinearInterpolator()
        playTogether(scaleX, scaleY)
    }

    val elevation = ValueAnimator.ofFloat(properties.elevationStart, properties.elevationEnd)
    elevation.addUpdateListener { this.elevation = (it.animatedValue as? Float) ?: properties.elevationStart }
    elevation.startDelay = halfDuration
    elevation.duration = 100

    val translateDown = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, upTo, properties.yTranslationEnd)
    translateDown.startDelay = halfDuration
    translateDown.duration = halfDuration

    return AnimatorSet().apply {
        playTogether(
                translateUp,
                scale,
                elevation,
                translateDown,
        )
    }
}

fun View.pullUpAnimation(leapDuration: Long, properties: AnimationProperties): AnimatorSet {
    val pullDuration = leapDuration / 2
    val delayDuration = leapDuration / 2

    val translateUp = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslationStart, properties.yTranslationEnd)
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scaleStart, properties.scaleEnd)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scaleStart, properties.scaleEnd)
    val elevation = ValueAnimator.ofFloat(properties.elevationStart, properties.elevationEnd)
    elevation.addUpdateListener { this.elevation = (it.animatedValue as? Float) ?: properties.elevationStart }

    return AnimatorSet().apply {
        startDelay = delayDuration
        duration = pullDuration
        interpolator = DecelerateInterpolator()
        playTogether(translateUp, scaleX, scaleY, elevation)
    }
}

fun View.pullDownAnimation(leapDuration: Long, properties: AnimationProperties): AnimatorSet {
    val pullDuration = leapDuration / 2
    val delayDuration = leapDuration / 4

    val translate = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, properties.yTranslationStart, properties.yTranslationEnd)
    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, properties.scaleStart, properties.scaleEnd)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, properties.scaleStart, properties.scaleEnd)
    val elevation = ValueAnimator.ofFloat(properties.elevationStart, properties.elevationEnd)
    elevation.addUpdateListener { this.elevation = (it.animatedValue as? Float) ?: properties.elevationStart }

    return AnimatorSet().apply {
        startDelay = delayDuration
        duration = pullDuration
        interpolator = DecelerateInterpolator()
        playTogether(translate, scaleX, scaleY, elevation)
    }
}