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

class WalletCardsWidget(
    val leapfrogWidget: LeapfrogWidget,
    private val deviceScaleFactor: Float = 1f,
    val getTopOfAnchorViewForActivateState: () -> Float,
) {

    private val animDuration: Long = 400

    var currentState: WidgetState? = null
        private set

    fun toWelcome(
        animate: Boolean = true,
        onEnd: () -> Unit = {},
    ) {
        if (currentState == WidgetState.WELCOME) return

        currentState = WidgetState.WELCOME

        val animator = createAnimator(animate, onEnd = onEnd)
        animator.playTogether(createAnimators(::createWelcomeProperties))
        leapfrogWidget.fold { animator.start() }
    }

    fun toFolded(
        animate: Boolean = true,
        duration: Long = animDuration,
        onEnd: () -> Unit = {},
    ) {
        if (currentState == WidgetState.FOLDED) return

        currentState = WidgetState.FOLDED

        val animator = createAnimator(animate, duration) {
            leapfrogWidget.fold(animate, onEnd)
        }
        animator.playTogether(createAnimators(::createLeapfrogProperties))
        animator.start()
    }

    fun toFan(
        animate: Boolean = true,
        onEnd: () -> Unit = {},
    ) {
        if (currentState == WidgetState.FAN) return

        currentState = WidgetState.FAN

        val animator = createAnimator(animate, onEnd = onEnd)
        animator.playTogether(createAnimators(::createFanProperties))
        leapfrogWidget.fold { animator.start() }
    }

    fun toLeapfrog(
        animate: Boolean = true,
        onEndFold: () -> Unit = {},
        onEnd: () -> Unit = {},
    ) {
        if (currentState == WidgetState.LEAPFROG) return

        currentState = WidgetState.LEAPFROG

        val onAnimatorEnd = {
            leapfrogWidget.fold(animate) {
                leapfrogWidget.initViews()
                onEndFold()
                leapfrogWidget.unfold(animate, onEnd)
            }
        }
        val animator = createAnimator(animate, onEnd = onAnimatorEnd)
        animator.playTogether(createAnimators(::createLeapfrogProperties))
        animator.start()
    }

    private fun createAnimators(propertyFactory: (BackupCardType) -> CardProperties): List<ObjectAnimator> {
        return getCardTypesTypes().map { createCardAnimator(it, propertyFactory(it)) }
    }

    @Suppress("MagicNumber")
    private fun getCardTypesTypes(): List<BackupCardType> = when (leapfrogWidget.getViewsCount()) {
        2 -> listOf(BackupCardType.ORIGIN, BackupCardType.FIRST_BACKUP)
        3 -> listOf(BackupCardType.ORIGIN, BackupCardType.FIRST_BACKUP, BackupCardType.SECOND_BACKUP)
        else -> throw UnsupportedOperationException()
    }

    private fun createAnimator(animate: Boolean, duration: Long = animDuration, onEnd: () -> Unit): AnimatorSet {
        return AnimatorSet().apply {
            this.duration = if (animate) duration else 0
            doOnEnd { onEnd() }
        }
    }

    private fun createCardAnimator(
        cardType: BackupCardType,
        properties: CardProperties,
    ): ObjectAnimator {
        val view = getLeapViewByCard(cardType).view
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            *properties.createValuesHolders().toTypedArray(),
        )
        view.elevation = properties.elevation
        return animator
    }

    @Suppress("MagicNumber")
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

    @Suppress("MagicNumber")
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

    @Suppress("MagicNumber")
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

    @Throws(UnsupportedOperationException::class)
    fun getOriginCardView(): ImageView {
        return getLeapViewByCard(BackupCardType.ORIGIN).view as ImageView
    }

    @Throws(UnsupportedOperationException::class)
    fun getFirstBackupCardView(): ImageView {
        return getLeapViewByCard(BackupCardType.FIRST_BACKUP).view as ImageView
    }

    @Throws(UnsupportedOperationException::class)
    fun getSecondBackupCardView(): ImageView {
        return getLeapViewByCard(BackupCardType.SECOND_BACKUP).view as ImageView
    }

    @Suppress("MagicNumber")
    @Throws(UnsupportedOperationException::class)
    private fun getLeapViewByCard(cardType: BackupCardType): LeapView {
        fun getByIndex(index: Int): LeapView {
            return leapfrogWidget.getViewByPosition(leapfrogWidget.getViewPositionByIndex(index))
        }

        return when (leapfrogWidget.getViewsCount()) {
            2 -> when (cardType) {
                BackupCardType.ORIGIN -> getByIndex(1)
                BackupCardType.FIRST_BACKUP -> getByIndex(0)
                BackupCardType.SECOND_BACKUP -> throw UnsupportedOperationException()
            }
            3 -> when (cardType) {
                BackupCardType.ORIGIN -> getByIndex(2)
                BackupCardType.FIRST_BACKUP -> getByIndex(1)
                BackupCardType.SECOND_BACKUP -> getByIndex(0)
            }
            else -> throw UnsupportedOperationException()
        }
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
