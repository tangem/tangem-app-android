package com.tangem.features.storiesv2.impl.storybook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoriesV2Prefetcher
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.StoriesV2StorybookComponent
import com.tangem.features.storiesv2.StoryV2ActionTarget
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.features.storiesv2.impl.content.BundledStoryV2ContentSource
import com.tangem.features.storiesv2.impl.content.StoryV2ContentRepository
import com.tangem.features.storiesv2.impl.content.StoryV2PreviewContent
import com.tangem.features.storiesv2.impl.content.remote.StoryV2Registry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

@Suppress("LongParameterList")
internal class DefaultStoriesV2StorybookComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    private val previewContent: StoryV2PreviewContent,
    private val bundledSource: BundledStoryV2ContentSource,
    private val contentRepository: StoryV2ContentRepository,
    private val prefetcher: StoriesV2Prefetcher,
    private val playerFactory: StoriesV2Component.Factory,
) : StoriesV2StorybookComponent, AppComponentContext by appComponentContext {

    private val lastResult = MutableStateFlow<String?>(null)

    /** Keyed by story type name, so a remote story plays through the same path production takes. */
    private val playerNavigation = SlotNavigation<String>()

    private val playerSlot = childSlot(
        source = playerNavigation,
        serializer = String.serializer(),
        key = "storiesV2StorybookPlayer",
        // The player registers its own back callback, so it closes the way it does in production instead of
        // being popped from the outside.
        handleBackButton = false,
        childFactory = { typeName, componentContext ->
            playerFactory.create(
                context = childByContext(componentContext),
                params = StoriesV2Component.Params(
                    type = StoryV2Type.valueOf(typeName),
                    source = StoryV2Source.STORYBOOK,
                    // Every action ends the story, so the result line shows what was tapped.
                    onAction = StoriesV2PendingAction::end,
                    onResult = ::onResult,
                ),
            )
        },
    )

    init {
        // Until a real entry point exists, opening the showcase is the moment a remote story becomes known.
        prefetchRemoteStories()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val slot by playerSlot.subscribeAsState()
        val result by lastResult.collectAsStateWithLifecycle()

        val player = slot.child?.instance
        if (player != null) {
            player.Content(Modifier.fillMaxSize())
        } else {
            StoriesV2StorybookScreen(
                lastResult = result,
                remoteStories = StoryV2Registry.remoteTypes,
                onPresetClick = ::onPresetClick,
                onRemoteStoryClick = ::onRemoteStoryClick,
                onPrefetchClick = ::onPrefetchClick,
                onClearRemoteClick = ::onClearRemoteClick,
                modifier = modifier,
            )
        }
    }

    private fun onPresetClick(preset: StoryV2Preset) {
        // The shipped preset deliberately goes through the normal content path; every other one overrides it.
        previewContent.composition = if (preset == StoryV2Preset.ONBOARDING) {
            null
        } else {
            StoryV2Preset.build(preset = preset, bundled = bundledSource)
        }
        lastResult.value = null
        playerNavigation.activate(StoryV2Type.ONBOARDING.name)
    }

    private fun onRemoteStoryClick(type: StoryV2Type) {
        previewContent.composition = null
        lastResult.value = null
        playerNavigation.activate(type.name)
    }

    private fun onPrefetchClick() {
        prefetchRemoteStories()
        lastResult.value = "Prefetch started"
    }

    /** Leaves the next open with nothing on the device, which is how the loading screen and the retry get seen. */
    private fun onClearRemoteClick() {
        componentScope.launch {
            contentRepository.clearRemoteContent()
            lastResult.value = "Remote stories cleared"
        }
    }

    private fun prefetchRemoteStories() {
        StoryV2Registry.remoteTypes.forEach(prefetcher::prefetch)
    }

    private fun onResult(result: StoriesV2Result) {
        previewContent.composition = null
        lastResult.value = result.describe()
        playerNavigation.dismiss()
    }

    @AssistedFactory
    interface Factory : StoriesV2StorybookComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultStoriesV2StorybookComponent
    }
}

private fun StoriesV2Result.describe(): String = when (this) {
    StoriesV2Result.Dismissed -> "Dismissed"
    StoriesV2Result.Completed -> "Completed"
    is StoriesV2Result.ActionInvoked -> "Action \"$actionId\" → ${target.describe()}"
    is StoriesV2Result.Unavailable -> "Unavailable: ${reason.name}"
}

private fun StoryV2ActionTarget.describe(): String = when (this) {
    is StoryV2ActionTarget.Screen -> "screen $key"
    is StoryV2ActionTarget.Web -> "web $url"
    is StoryV2ActionTarget.Deeplink -> "deeplink $uri"
}