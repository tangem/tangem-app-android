package com.tangem.tap.features.onboarding.products.wallet.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapView
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapViewState
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget

class BackupCardsWidget(
    val leapfrogWidget: LeapfrogWidget,
    private val deviceScaleFactor: Float = 1f,
    val getTopOfAnchorViewForActivateState: () -> Float,
) {

    var currentState: WidgetState? = null

    fun toWelcome(animate: Boolean = true, onEnd: () -> Unit = {}) {
        if (currentState == WidgetState.WELCOME) return

        currentState = WidgetState.WELCOME

        val animator = createAnimator(animate, onEnd)
        animator.playTogether(
            createAnimator(BackupCardType.ORIGIN, createWelcomeProperties(BackupCardType.ORIGIN)),
            createAnimator(BackupCardType.FIRST_BACKUP, createWelcomeProperties(BackupCardType.FIRST_BACKUP)),
            createAnimator(BackupCardType.SECOND_BACKUP, createWelcomeProperties(BackupCardType.SECOND_BACKUP))
        )
        leapfrogWidget.fold { animator.start() }
    }

    fun toFolded(animate: Boolean = true, onEnd: () -> Unit = {}) {
        if (currentState == WidgetState.FOLDED) return

        currentState = WidgetState.FOLDED

        val animator = createAnimator(animate, onEnd)
        animator.playTogether(
            createAnimator(BackupCardType.ORIGIN, createLeapfrogProperties(BackupCardType.ORIGIN)),
            createAnimator(BackupCardType.FIRST_BACKUP, createLeapfrogProperties(BackupCardType.FIRST_BACKUP)),
            createAnimator(BackupCardType.SECOND_BACKUP, createLeapfrogProperties(BackupCardType.SECOND_BACKUP))
        )
        leapfrogWidget.fold(animate) { animator.start() }
    }

    fun toFan(animate: Boolean = true, onEnd: () -> Unit = {}) {
        if (currentState == WidgetState.FAN) return

        currentState = WidgetState.FAN

        val animator = createAnimator(animate, onEnd)
        animator.playTogether(
            createAnimator(BackupCardType.ORIGIN, createFanProperties(BackupCardType.ORIGIN)),
            createAnimator(BackupCardType.FIRST_BACKUP, createFanProperties(BackupCardType.FIRST_BACKUP)),
            createAnimator(BackupCardType.SECOND_BACKUP, createFanProperties(BackupCardType.SECOND_BACKUP))
        )
        leapfrogWidget.fold { animator.start() }
    }

    fun toLeapfrog(animate: Boolean = true, onEnd: () -> Unit = {}) {
        currentState = WidgetState.LEAPFROG

        val animator = createAnimator(animate, onEnd)
        animator.playTogether(
            createAnimator(BackupCardType.ORIGIN, createLeapfrogProperties(BackupCardType.ORIGIN)),
            createAnimator(BackupCardType.FIRST_BACKUP, createLeapfrogProperties(BackupCardType.FIRST_BACKUP)),
            createAnimator(BackupCardType.SECOND_BACKUP, createLeapfrogProperties(BackupCardType.SECOND_BACKUP))
        )
        leapfrogWidget.fold {
            animator.doOnEnd {
//                leapfrogWidget.initViews()
                leapfrogWidget.unfold()
            }
            animator.start()
        }
    }

    private fun createAnimator(animate: Boolean, onEnd: () -> Unit): AnimatorSet {
        return AnimatorSet().apply {
            duration = if (animate) 400 else 0
            doOnEnd { onEnd() }
        }
    }

    private fun createAnimator(
        cardType: BackupCardType,
        properties: CardProperties,
    ): ObjectAnimator {
        val view = getLeapViewByCardNumber(cardType).view
        val animator = ObjectAnimator.ofPropertyValuesHolder(view,
            *properties.createValuesHolders().toTypedArray())
        view.elevation = properties.elevation
        return animator
    }

    private fun createWelcomeProperties(cardType: BackupCardType): CardProperties {
        return when (cardType) {
            BackupCardType.ORIGIN -> CardProperties(
                xTranslation = 440f * deviceScaleFactor,
                yTranslation = 70f,
                rotation = 75f,
                elevation = 2f,
                scale = .85f * deviceScaleFactor,
            )
            BackupCardType.FIRST_BACKUP -> CardProperties(
                xTranslation = 10f * deviceScaleFactor,
                yTranslation = -30f,
                rotation = 100f,
                elevation = 1f,
                scale = .85f * deviceScaleFactor,
            )
            BackupCardType.SECOND_BACKUP -> CardProperties(
                xTranslation = -440f * deviceScaleFactor,
                yTranslation = -30f,
                rotation = 70f,
                elevation = 0f,
                scale = .85f * deviceScaleFactor,
            )
        }
    }

    private fun createFanProperties(cardType: BackupCardType): CardProperties {
        return when (cardType) {
            BackupCardType.ORIGIN -> CardProperties(
                xTranslation = 0f,
                yTranslation = 30f,
                rotation = 5f,
                elevation = 2f,
                scale = 1f * deviceScaleFactor,
            )
            BackupCardType.FIRST_BACKUP -> CardProperties(
                xTranslation = 0f,
                yTranslation = -50f,
                rotation = -5f,
                elevation = 1f,
                scale = 0.9f * deviceScaleFactor,
            )
            BackupCardType.SECOND_BACKUP -> CardProperties(
                xTranslation = 10f,
                yTranslation = -120f,
                rotation = -20f,
                elevation = 0f,
                scale = 0.8f * deviceScaleFactor,
            )
        }
    }

    private fun createLeapfrogProperties(cardType: BackupCardType): CardProperties {
        return when (cardType) {
            BackupCardType.ORIGIN -> CardProperties(
                xTranslation = 0f,
                yTranslation = 0f,
                rotation = 0f,
                elevation = 2f,
                scale = 1f * deviceScaleFactor,
            )
            BackupCardType.FIRST_BACKUP -> CardProperties(
                xTranslation = 0f,
                yTranslation = 0f,
                rotation = 0f,
                elevation = 1f,
                scale = 0.9f * deviceScaleFactor,
            )
            BackupCardType.SECOND_BACKUP -> CardProperties(
                xTranslation = 0f,
                yTranslation = 0f,
                rotation = 0f,
                elevation = 0f,
                scale = 0.8f * deviceScaleFactor,
            )
        }
    }

    private fun createActivateProperties(cardType: BackupCardType): CardProperties {
        val topOfAnchorView = getTopOfAnchorViewForActivateState()
        val twinProperties = CardProperties.from(getLeapViewByCardNumber(cardType).state)
        return twinProperties.copy(
            xTranslation = twinProperties.xTranslation,
            yTranslation = twinProperties.yTranslation - topOfAnchorView,
            rotation = 0f,
            scale = twinProperties.scale - 0.4f,
        )
    }

    private fun getLeapViewByCardNumber(cardType: BackupCardType): LeapView {
        return when (cardType) {
            BackupCardType.ORIGIN -> leapfrogWidget.getViewByPosition(0)
            BackupCardType.FIRST_BACKUP -> leapfrogWidget.getViewByPosition(1)
            BackupCardType.SECOND_BACKUP -> leapfrogWidget.getViewByPosition(2)
        }
    }

    fun getOriginCardView(): ImageView {
        return getLeapViewByCardNumber(BackupCardType.ORIGIN).view as ImageView
    }

    fun getFirstBackupCardView(): ImageView {
        return getLeapViewByCardNumber(BackupCardType.FIRST_BACKUP).view as ImageView
    }

    fun getSecondBackupCardView(): ImageView {
        return getLeapViewByCardNumber(BackupCardType.SECOND_BACKUP).view as ImageView
    }

    enum class WidgetState { WELCOME, FOLDED, FAN, LEAPFROG }
}


enum class BackupCardType { ORIGIN, FIRST_BACKUP, SECOND_BACKUP }


private data class CardProperties(
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
        fun from(leapViewState: LeapViewState): CardProperties {
            val leapViewProperties = leapViewState.properties

            return CardProperties(
                yTranslation = leapViewProperties.yTranslation,
                elevation = leapViewProperties.elevationEnd,
                scale = leapViewProperties.scale,
            )
        }
    }
}