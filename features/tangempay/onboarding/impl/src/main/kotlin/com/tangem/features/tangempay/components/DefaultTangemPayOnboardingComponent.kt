package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.model.TangemPayOnboardingModel
import com.tangem.features.tangempay.ui.TandemPayOnboardingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TangemPayOnboardingComponent.Params,
) : TangemPayOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayOnboardingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        TandemPayOnboardingScreen(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory : TangemPayOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayOnboardingComponent.Params,
        ): DefaultTangemPayOnboardingComponent
    }
}