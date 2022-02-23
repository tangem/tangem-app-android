package com.tangem.tap.domain.twins

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import androidx.core.animation.doOnEnd
import com.tangem.common.extensions.VoidCallback
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapView
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapViewState
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget


/**
[REDACTED_AUTHOR]
 */
class TwinsCardWidget(
    val leapfrogWidget: LeapfrogWidget,
    private val deviceScaleFactor: Float = 1f,
    val getTopOfAnchorViewForActivateState: () -> Float
) {

    var state: TwinCardsWidgetState? = null
        private set

    private val animationDuration = 300L

    init {
        if (leapfrogWidget.getViewsCount() != 2) throw UnsupportedOperationException()
    }

    fun toWelcome(animate: Boolean = true, onEnd: VoidCallback = {}) {
        if (state == TwinCardsWidgetState.Welcome) return

        state = TwinCardsWidgetState.Welcome
        val animator = createAnimator(animate)
        animator.playTogether(
            createAnimator(TwinCardNumber.First, createWelcomeProperties(TwinCardNumber.First)),
            createAnimator(TwinCardNumber.Second, createWelcomeProperties(TwinCardNumber.Second))
        )
        animator.doOnEnd { onEnd() }
        leapfrogWidget.fold(animate) { animator.start() }
    }

    fun toLeapfrog(animate: Boolean = true, onEnd: VoidCallback = {}) {
        if (state == TwinCardsWidgetState.Leapfrog) return

        state = TwinCardsWidgetState.Leapfrog
        val animator = createAnimator(animate)
        animator.playTogether(
            createAnimator(TwinCardNumber.First, createLeapfrogProperties(TwinCardNumber.First)),
            createAnimator(TwinCardNumber.Second, createLeapfrogProperties(TwinCardNumber.Second))
        )
        animator.doOnEnd {
            leapfrogWidget.initViews()
            leapfrogWidget.unfold(animate, onEnd)
        }
        animator.start()
    }

    fun toActivate(animate: Boolean = true, onEnd: VoidCallback = {}) {
        if (state == TwinCardsWidgetState.Activate) return

        state = TwinCardsWidgetState.Activate
        val animator = createAnimator(animate)
        animator.playTogether(
            createAnimator(TwinCardNumber.First, createActivateProperties(TwinCardNumber.First)),
            createAnimator(TwinCardNumber.Second, createActivateProperties(TwinCardNumber.Second))
        )
        animator.doOnEnd { onEnd() }
        animator.start()
    }

    private fun createAnimator(animate: Boolean): AnimatorSet {
        return AnimatorSet().apply {
            duration = if (animate) animationDuration else 0
        }
    }

    private fun createAnimator(cardNumber: TwinCardNumber, properties: TwinsCardProperties): ObjectAnimator {
        val view = getLeapViewByCardNumber(cardNumber).view
        val animator = ObjectAnimator.ofPropertyValuesHolder(view, *properties.createValuesHolders().toTypedArray())
        view.elevation = properties.elevation
        return animator
    }

    private fun createWelcomeProperties(cardNumber: TwinCardNumber): TwinsCardProperties {
        return when (cardNumber) {
            TwinCardNumber.First -> {
                TwinsCardProperties(
                    xTranslation = -120f * deviceScaleFactor,
                    yTranslation = -220f * deviceScaleFactor,
                    rotation = -3f,
                    elevation = 1f,
                    scale = deviceScaleFactor,
                )
            }
            TwinCardNumber.Second -> {
                TwinsCardProperties(
                    xTranslation = 170f * deviceScaleFactor,
                    yTranslation = 170f * deviceScaleFactor,
                    rotation = -3f,
                    elevation = 0f,
                    scale = deviceScaleFactor,
                )
            }
        }
    }

    private fun createLeapfrogProperties(cardNumber: TwinCardNumber): TwinsCardProperties {
        val twinProperties = TwinsCardProperties.from(getLeapViewByCardNumber(cardNumber).state)
        return twinProperties.copy(
            xTranslation = 0f,
            yTranslation = 0f,
            rotation = 0f,
            scale = 1f,
        )
    }

    private fun createActivateProperties(cardNumber: TwinCardNumber): TwinsCardProperties {
        val topOfAnchorView = getTopOfAnchorViewForActivateState()
        val twinProperties = TwinsCardProperties.from(getLeapViewByCardNumber(cardNumber).state)
        return twinProperties.copy(
            xTranslation = twinProperties.xTranslation,
            yTranslation = twinProperties.yTranslation - topOfAnchorView,
            rotation = 0f,
            scale = (twinProperties.scale - 0.5f) * deviceScaleFactor,
        )
    }

    private fun getLeapViewByCardNumber(cardNumber: TwinCardNumber): LeapView {
        return when (cardNumber) {
            TwinCardNumber.First -> leapfrogWidget.getViewByPosition(0)
            TwinCardNumber.Second -> leapfrogWidget.getViewByPosition(1)
        }
    }
}

enum class TwinCardsWidgetState {
    Welcome, Leapfrog, Activate
}

private data class TwinsCardProperties(
    val xTranslation: Float = 0f,
    val yTranslation: Float = 0f,
    val rotation: Float = 0f,
    val elevation: Float = 0f,
    val scale: Float = 1.1f,
) {

    fun createValuesHolders(): List<PropertyValuesHolder> {
        return listOf(
            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, xTranslation),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, yTranslation),
            PropertyValuesHolder.ofFloat(View.ROTATION, rotation),
            PropertyValuesHolder.ofFloat(View.SCALE_X, scale),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, scale),
        )
    }

    companion object {
        fun from(leapViewState: LeapViewState): TwinsCardProperties {
            val leapViewProperties = leapViewState.properties

            return TwinsCardProperties(
                yTranslation = leapViewProperties.yTranslation,
                elevation = leapViewProperties.elevationEnd,
                scale = leapViewProperties.scale,
            )
        }
    }
}