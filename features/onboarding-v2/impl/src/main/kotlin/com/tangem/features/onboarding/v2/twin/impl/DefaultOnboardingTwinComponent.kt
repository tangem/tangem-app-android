package com.tangem.features.onboarding.v2.twin.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.model.OnboardingTwinModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardingTwinComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: OnboardingTwinComponent.Params,
) : OnboardingTwinComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingTwinModel = getOrCreateModel(params)

    init {
        params.titleProvider.changeTitle(resourceReference(R.string.twins_recreate_toolbar))
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler { model.onBack() }

        // TODO
    }

    @AssistedFactory
    interface Factory : OnboardingTwinComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingTwinComponent.Params,
        ): DefaultOnboardingTwinComponent
    }
}