package com.tangem.features.onboarding.v2.visa.impl.child.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaActivationInput
import com.tangem.domain.visa.model.VisaCardWalletDataToSignRequest
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.OnboardingVisaWelcomeModel
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui.OnboardingVisaWelcome
import com.tangem.features.onboarding.v2.visa.impl.common.ActivationReadyEvent
import kotlinx.coroutines.launch

internal class OnboardingVisaWelcomeComponent(
    appComponentContext: AppComponentContext,
    config: Config,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaWelcomeModel = getOrCreateModel(config)

    init {
        componentScope.launch {
            model.onDone.collect { params.onDone(it) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingVisaWelcome(
            modifier = modifier,
            state = state,
        )
    }

    sealed class Config {
        data object Welcome : Config()

        data class WelcomeBack(
            val activationInput: VisaActivationInput,
            val dataToSignRequest: VisaCardWalletDataToSignRequest,
            val scanResponse: ScanResponse,
        ) : Config()
    }

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onDone: (DoneEvent) -> Unit,
    )

    sealed class DoneEvent {
        data object WelcomeDone : DoneEvent()

        data class WelcomeBackDone(
            val activationReadyEvent: ActivationReadyEvent,
        ) : DoneEvent()
    }
}