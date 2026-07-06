package com.tangem.features.virtualaccount.onboarding.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.virtualaccount.onboarding.model.VirtualAccountOnboardingModel
import com.tangem.features.virtualaccount.onboarding.ui.VirtualAccountOnboardingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultVirtualAccountOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: VirtualAccountOnboardingComponent.Params,
) : VirtualAccountOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: VirtualAccountOnboardingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        VirtualAccountOnboardingScreen(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory : VirtualAccountOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: VirtualAccountOnboardingComponent.Params,
        ): DefaultVirtualAccountOnboardingComponent
    }
}