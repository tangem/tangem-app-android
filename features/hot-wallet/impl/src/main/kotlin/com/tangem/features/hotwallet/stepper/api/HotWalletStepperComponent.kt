package com.tangem.features.hotwallet.stepper.api

import androidx.annotation.IntRange
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.coroutines.flow.StateFlow

interface HotWalletStepperComponent : ComposableContentComponent {

    data class StepperUM(
        @IntRange(from = 0) val currentStep: Int,
        @IntRange(from = 0) val steps: Int,
        val title: TextReference,
        val showBackButton: Boolean,
        val showSkipButton: Boolean,
        val showFeedbackButton: Boolean,
    ) {
        companion object {
            fun initialState() = StepperUM(
                currentStep = 0,
                steps = 0,
                title = TextReference.EMPTY,
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )
        }
    }

    interface ModelCallback {
        fun onBackClick()
        fun onSkipClick()
    }

    class Params(
        val initState: StepperUM,
        val callback: ModelCallback,
    )

    val state: StateFlow<StepperUM>

    interface Factory : ComponentFactory<Params, HotWalletStepperComponent>
}