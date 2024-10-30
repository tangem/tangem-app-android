package com.tangem.features.onboarding.v2.stepper.api

import androidx.annotation.IntRange
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.scan.ScanResponse
import kotlinx.coroutines.flow.StateFlow

internal interface OnboardingStepperComponent : ComposableContentComponent {

    data class StepperState(
        @IntRange(from = 0) val currentStep: Int,
        @IntRange(from = 0) val steps: Int,
        val title: TextReference,
    )

    class Params(
        val initState: StepperState,
        val popBack: () -> Unit,
        val scanResponse: ScanResponse,
    )

    val state: StateFlow<StepperState>

    fun changeStep(step: Int)
    fun nextStep()
    fun changeTitle(title: TextReference)

    interface Factory : ComponentFactory<Params, OnboardingStepperComponent>
}
