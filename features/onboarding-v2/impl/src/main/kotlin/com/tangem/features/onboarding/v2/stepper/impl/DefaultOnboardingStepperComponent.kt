package com.tangem.features.onboarding.v2.stepper.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.common.TapWorkarounds.isVisa
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.onboarding.v2.stepper.api.OnboardingStepperComponent
import com.tangem.features.onboarding.v2.stepper.impl.ui.OnboardingStepper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class DefaultOnboardingStepperComponent @AssistedInject constructor(
    @Assisted val context: AppComponentContext,
    @Assisted val params: OnboardingStepperComponent.Params,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val analyticsHandler: AnalyticsEventHandler,
) : OnboardingStepperComponent, AppComponentContext by context {

    override val state = instanceKeeper.getOrCreateSimple { MutableStateFlow(params.initState) }

    private fun openSupport() {
        analyticsHandler.send(
            AnalyticsEvent(
                category = "Onboarding",
                event = "Button - Chat",
            ),
        )

        componentScope.launch {
            val cardInfo = getCardInfoUseCase(params.scanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(
                if (params.scanResponse.card.isVisa) {
                    FeedbackEmailType.Visa.Activation(cardInfo)
                } else {
                    FeedbackEmailType.DirectUserRequest(cardInfo)
                },
            )
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by state.collectAsStateWithLifecycle()

        OnboardingStepper(
            state = uiState,
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