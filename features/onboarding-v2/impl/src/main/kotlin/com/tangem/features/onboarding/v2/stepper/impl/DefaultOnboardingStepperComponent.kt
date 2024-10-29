package com.tangem.features.onboarding.v2.stepper.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.onboarding.v2.stepper.api.OnboardingStepperComponent
import com.tangem.features.onboarding.v2.stepper.impl.ui.OnboardingStepper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DefaultOnboardingStepperComponent @AssistedInject constructor(
    @Assisted val context: AppComponentContext,
    @Assisted val params: OnboardingStepperComponent.Params,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) : OnboardingStepperComponent, AppComponentContext by context {

    override val state = instanceKeeper.getOrCreateSimple { MutableStateFlow(params.initState) }

    override fun changeStep(step: Int) {
        state.update { it.copy(currentStep = step.coerceAtLeast(minimumValue = 0)) }
    }

    override fun nextStep() {
        state.update { state ->
            if (state.currentStep < state.steps - 1) {
                state.copy(currentStep = state.currentStep + 1)
            } else {
                state
            }
        }
    }

    override fun changeTitle(title: TextReference) {
        state.update { it.copy(title = title) }
    }

    private fun openSupport() {
        componentScope.launch {
            val cardInfo = getCardInfoUseCase(params.scanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()

        OnboardingStepper(
            state = state,
            onBackClick = remember(this) { params.popBack },
            onSupportButtonClick = remember(this) { ::openSupport },
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : OnboardingStepperComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingStepperComponent.Params,
        ): DefaultOnboardingStepperComponent
    }
}
