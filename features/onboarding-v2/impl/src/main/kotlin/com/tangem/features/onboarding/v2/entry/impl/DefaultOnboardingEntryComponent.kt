package com.tangem.features.onboarding.v2.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.ui.OnboardingEntry
import com.tangem.features.onboarding.v2.stepper.api.OnboardingStepperComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardingEntryComponent @AssistedInject constructor(
    @Assisted val context: AppComponentContext,
    @Assisted val params: OnboardingEntryComponent.Params,
    stepperFactory: OnboardingStepperComponent.Factory,
) : OnboardingEntryComponent, AppComponentContext by context {

    private val stepperComponent = stepperFactory.create(
        context = child("stepper"),
        params = OnboardingStepperComponent.Params(
            scanResponse = params.scanResponse,
            initState = OnboardingStepperComponent.StepperState(
                currentStep = 0,
                steps = 3,
                title = stringReference(""),
            ),
            popBack = { router.pop() },
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        OnboardingEntry(
            modifier = modifier,
            stepperContent = { modifierParam ->
                stepperComponent.Content(modifierParam)
            },
        )
    }

    @AssistedFactory
    interface Factory : OnboardingEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingEntryComponent.Params,
        ): DefaultOnboardingEntryComponent
    }
}
