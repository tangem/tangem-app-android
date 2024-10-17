package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.managetokens.model.OnboardingManageTokensModel
import com.tangem.features.managetokens.ui.OnboardingManageTokensContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardingManageTokensComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: OnboardingManageTokensComponent.Params,
) : OnboardingManageTokensComponent, AppComponentContext by context {

    private val model: OnboardingManageTokensModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        OnboardingManageTokensContent(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory : OnboardingManageTokensComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingManageTokensComponent.Params,
        ): DefaultOnboardingManageTokensComponent
    }
}
