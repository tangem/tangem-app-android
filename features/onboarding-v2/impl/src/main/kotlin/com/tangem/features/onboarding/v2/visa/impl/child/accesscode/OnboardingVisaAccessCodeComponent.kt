package com.tangem.features.onboarding.v2.visa.impl.child.accesscode

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model.OnboardingVisaAccessCodeModel
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.OnboardingVisaAccessCode
import com.tangem.features.onboarding.v2.visa.impl.common.ActivationReadyEvent
import kotlinx.coroutines.launch

internal class OnboardingVisaAccessCodeComponent(
    appComponentContext: AppComponentContext,
    config: Config,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaAccessCodeModel = getOrCreateModel(config)

    init {
        componentScope.launch {
            model.onBack.collect { params.childParams.onBack() }
        }
        componentScope.launch {
            params.childParams.parentBackEvent.collect {
                model.onBack()
            }
        }
        componentScope.launch {
            model.onDone.collect { params.onDone(it) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = model::onBack)

        DisableScreenshotsDisposableEffect()

        OnboardingVisaAccessCode(state, modifier)
    }

    data class Config(
        val scanResponse: ScanResponse,
    )

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onDone: (ActivationReadyEvent) -> Unit,
    )
}