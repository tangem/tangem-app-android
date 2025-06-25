package com.tangem.features.welcome.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.welcome.WelcomeComponent
import com.tangem.features.welcome.impl.model.WelcomeModel
import com.tangem.features.welcome.impl.ui.Welcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWelcomeComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: WelcomeComponent.Params,
) : WelcomeComponent, AppComponentContext by context {

    private val model: WelcomeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Welcome(
            modifier = modifier,
            state = state,
        )
    }

    @AssistedFactory
    interface Factory : WelcomeComponent.Factory {
        override fun create(context: AppComponentContext, params: WelcomeComponent.Params): DefaultWelcomeComponent
    }
}