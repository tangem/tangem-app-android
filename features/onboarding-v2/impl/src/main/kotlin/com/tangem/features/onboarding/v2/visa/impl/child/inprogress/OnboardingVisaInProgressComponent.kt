package com.tangem.features.onboarding.v2.visa.impl.child.inprogress

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.model.OnboardingVisaInProgressModel
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.ui.OnboardingVisaInProgress
import com.tangem.features.onboarding.v2.visa.impl.route.OnboardingVisaRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class OnboardingVisaInProgressComponent(
    appComponentContext: AppComponentContext,
    config: Config,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaInProgressModel = getOrCreateModel(config)

    init {
        model.onDone
            .onEach { params.onDone(it) }
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingVisaInProgress(
            modifier = modifier,
        )
    }

    data class Config(
        val scanResponse: ScanResponse,
    )

    data class Params(
        val childParams: DefaultOnboardingVisaComponent.ChildParams,
        val onDone: (DoneEvent) -> Unit,
    ) {

        sealed class DoneEvent {
            data class NavigateTo(val route: OnboardingVisaRoute) : DoneEvent()
            data object Activated : DoneEvent()
        }
    }
}