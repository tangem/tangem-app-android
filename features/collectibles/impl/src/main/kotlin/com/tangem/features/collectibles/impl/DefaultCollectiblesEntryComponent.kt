package com.tangem.features.collectibles.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.childContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.features.collectibles.api.CollectiblesEntryComponent
import com.tangem.features.collectibles.impl.onboarding.CollectiblesOnboardingComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCollectiblesEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted @Suppress("UnusedPrivateProperty") private val params: Unit,
    onboardingComponentFactory: CollectiblesOnboardingComponent.Factory,
) : CollectiblesEntryComponent, AppComponentContext by context {

    private val onboardingComponent = onboardingComponentFactory.create(
        context = childByContext(childContext(key = ONBOARDING_CHILD_KEY)),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        onboardingComponent.Content(modifier = modifier)
    }

    @AssistedFactory
    interface Factory : CollectiblesEntryComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCollectiblesEntryComponent
    }

    private companion object {
        const val ONBOARDING_CHILD_KEY = "onboarding"
    }
}