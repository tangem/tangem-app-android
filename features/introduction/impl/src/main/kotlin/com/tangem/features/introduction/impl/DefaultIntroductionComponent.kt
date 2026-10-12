package com.tangem.features.introduction.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.model.IntroductionModel
import com.tangem.features.introduction.impl.ui.IntroductionScreen
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.StoryV2Type
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val STORIES_SLOT_KEY = "onboardingStories"

internal class DefaultIntroductionComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: IntroductionComponent.Params,
    private val storiesComponentFactory: StoriesV2Component.Factory,
) : IntroductionComponent, AppComponentContext by appComponentContext {

    private val model: IntroductionModel = getOrCreateModel(params)

    private val storiesSlot = childSlot(
        source = model.storiesNavigation,
        // Nothing above this slot is persisted, so a restored configuration could only reopen a looping story
        // nobody asked for.
        serializer = null,
        key = STORIES_SLOT_KEY,
        // The player registers its own back callback, so it closes the way it does everywhere else instead of
        // being popped from the outside.
        handleBackButton = false,
        childFactory = { _, componentContext ->
            storiesComponentFactory.create(
                context = childByContext(componentContext),
                params = StoriesV2Component.Params(
                    type = StoryV2Type.ONBOARDING,
                    source = StoryV2Source.ONBOARDING,
                    onAction = model::onStoryAction,
                    onResult = model::onStoryResult,
                ),
            )
        },
    )

    init {
        lifecycle.doOnResume { model.setVideoRunning(isRunning = true) }
        lifecycle.doOnPause { model.setVideoRunning(isRunning = false) }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stories by storiesSlot.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()

        // The story replaces the screen rather than covering it: two SurfaceViews in one window have no defined
        // order between their holes, and the Create wallet sheet is a dialog window that would keep back and
        // touches for itself as long as it stayed composed.
        val player = stories.child?.instance
        if (player != null) {
            player.Content(modifier.fillMaxSize())
        } else {
            IntroductionScreen(
                state = state,
                onSurfaceAvailable = model::attachSurface,
                onSurfaceRelease = model::detachSurface,
                videoPositionMs = model::videoPositionMs,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : IntroductionComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: IntroductionComponent.Params,
        ): DefaultIntroductionComponent
    }
}