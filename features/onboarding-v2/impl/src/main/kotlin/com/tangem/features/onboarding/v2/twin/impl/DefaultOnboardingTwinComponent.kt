package com.tangem.features.onboarding.v2.twin.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationHolder
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.model.OnboardingTwinModel
import com.tangem.features.onboarding.v2.twin.impl.ui.OnboardingTwin
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

internal class DefaultOnboardingTwinComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: OnboardingTwinComponent.Params,
) : OnboardingTwinComponent, AppComponentContext by appComponentContext, InnerNavigationHolder {

    private val model: OnboardingTwinModel = getOrCreateModel(params)

    init {
        params.titleProvider.changeTitle(resourceReference(R.string.twins_recreate_toolbar))
    }

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state: StateFlow<TwinInnerNavigationState> = model.innerNavigationState

        override fun pop(onComplete: (Boolean) -> Unit) {
            model.onBack()
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler { model.onBack() }

        val state by model.uiState.collectAsStateWithLifecycle()

        OnboardingTwin(
            state = state,
            modifier = modifier,
        )
    }

    data class TwinInnerNavigationState(
        override val stackSize: Int,
    ) : InnerNavigationState {
        override val stackMaxSize: Int? = 5
    }

    @AssistedFactory
    interface Factory : OnboardingTwinComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingTwinComponent.Params,
        ): DefaultOnboardingTwinComponent
    }
}