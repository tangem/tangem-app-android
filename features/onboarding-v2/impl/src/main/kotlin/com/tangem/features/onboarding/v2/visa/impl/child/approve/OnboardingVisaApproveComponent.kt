package com.tangem.features.onboarding.v2.visa.impl.child.approve

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
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.approve.model.OnboardingVisaApproveModel
import com.tangem.features.onboarding.v2.visa.impl.child.approve.ui.OnboardingVisaApprove
import kotlinx.coroutines.launch

internal class OnboardingVisaApproveComponent(
    appComponentContext: AppComponentContext,
    config: Config,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaApproveModel = getOrCreateModel(config)

    init {
        componentScope.launch {
            model.onDone.collect { params.onDone() }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingVisaApprove(
            state = state,
            modifier = modifier,
        )
    }

    data class Config(
        val visaDataForApprove: VisaDataForApprove,
        val scanResponse: ScanResponse,
    )

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onDone: () -> Unit,
    )
}