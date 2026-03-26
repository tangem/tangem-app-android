package com.tangem.features.virtualaccount.onboarding.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.virtualaccount.onboarding.api.VirtualAccountOnboardingComponent
import com.tangem.features.virtualaccount.onboarding.impl.model.VirtualAccountOnboardingModel
import com.tangem.features.virtualaccount.onboarding.impl.ui.VirtualAccountOnboardingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultVirtualAccountOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Unit,
) : VirtualAccountOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: VirtualAccountOnboardingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        VirtualAccountOnboardingScreen(
            state = uiState,
            onBackClick = model::onBackClick,
            onLearnMoreClick = {},
            onContinueClick = {},
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : VirtualAccountOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: Unit,
        ): DefaultVirtualAccountOnboardingComponent
    }
}