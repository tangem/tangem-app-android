package com.tangem.features.storiesv2.impl.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.impl.model.StoriesV2Model
import com.tangem.features.storiesv2.impl.ui.StoriesV2Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultStoriesV2Component @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: StoriesV2Component.Params,
) : StoriesV2Component, AppComponentContext by appComponentContext {

    private val model: StoriesV2Model = getOrCreateModel(params = params)

    init {
        // Leaving the app is a pause, not an exit: the story resumes from the same position, which is also what
        // keeps watched time honest and stops haptics firing at a screen nobody is looking at.
        lifecycle.doOnResume { model.setInForeground(inForeground = true) }
        lifecycle.doOnPause { model.setInForeground(inForeground = false) }

        backHandler.register(BackCallback { model.onBackClick() })
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val videoAspectRatio by model.videoAspectRatio.collectAsStateWithLifecycle()

        // Nothing is drawn until the composition resolves. That is not a loading state the viewer can reach: a
        // story that cannot be shown reports itself unavailable and the host moves on instead.
        state?.let { storyState ->
            StoriesV2Screen(
                state = storyState,
                handle = model,
                videoAspectRatio = videoAspectRatio,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : StoriesV2Component.Factory {
        override fun create(context: AppComponentContext, params: StoriesV2Component.Params): DefaultStoriesV2Component
    }
}