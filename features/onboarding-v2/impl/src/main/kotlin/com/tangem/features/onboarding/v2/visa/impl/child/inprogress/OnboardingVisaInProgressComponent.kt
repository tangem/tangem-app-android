package com.tangem.features.onboarding.v2.visa.impl.child.inprogress

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.model.OnboardingVisaInProgressModel
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.ui.OnboardingVisaInProgress
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OnboardingVisaInProgressComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingVisaInProgressModel = getOrCreateModel()

    init {
        model.onDone
            .onEach { params.onDone() }
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = remember(this) { { params.onBack() } })

        OnboardingVisaInProgress(
            modifier = modifier,
        )
    }

    data class Params(
        val onBack: () -> Unit,
        val onDone: () -> Unit,
    )
}