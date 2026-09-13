package com.tangem.features.introduction.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.model.IntroductionModel
import com.tangem.features.introduction.impl.ui.IntroductionScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultIntroductionComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: IntroductionComponent.Params,
) : IntroductionComponent, AppComponentContext by appComponentContext {

    private val model: IntroductionModel = getOrCreateModel(params)

    init {
        lifecycle.doOnResume { model.setVideoRunning(isRunning = true) }
        lifecycle.doOnPause { model.setVideoRunning(isRunning = false) }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        IntroductionScreen(
            state = state,
            onSurfaceAvailable = model::attachSurface,
            onSurfaceRelease = model::detachSurface,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : IntroductionComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: IntroductionComponent.Params,
        ): DefaultIntroductionComponent
    }
}