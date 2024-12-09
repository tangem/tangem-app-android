package com.tangem.features.onboarding.v2.done.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.done.impl.ui.OnboardingDone
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardingDoneComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: OnboardingDoneComponent.Params,
) : OnboardingDoneComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        OnboardingDone(
            onContinueClick = remember(this) { params.onDone },
        )
    }

    @AssistedFactory
    interface Factory : OnboardingDoneComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingDoneComponent.Params,
        ): DefaultOnboardingDoneComponent
    }
}