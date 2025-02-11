package com.tangem.feature.stories.impl.model

import com.tangem.common.ui.swapStoriesScreen.SwapStoriesFactory
import com.tangem.common.ui.swapStoriesScreen.SwapStoriesUM
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.ShouldShowSwapStoriesUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.feature.stories.api.StoriesComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class StoriesModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val shouldShowSwapStoriesUseCase: ShouldShowSwapStoriesUseCase,
) : Model() {
    val state: StateFlow<SwapStoriesUM> get() = _state

    private val params = paramsContainer.require<StoriesComponent.Params>()
    private val _state: MutableStateFlow<SwapStoriesUM> = MutableStateFlow(
        value = SwapStoriesUM.Empty,
    )

    init {
        initStories()
    }

    private fun openScreen(hideStories: Boolean = true) {
        modelScope.launch {
            if (hideStories) {
                shouldShowSwapStoriesUseCase.neverToShow()
            }
            router.pop()
            router.push(params.nextScreen)
        }
    }

    private fun initStories() {
        modelScope.launch {
            getStoryContentUseCase(params.storyId).fold(
                ifLeft = {
                    Timber.e("Unable to load stories for ${StoryContentIds.STORY_FIRST_TIME_SWAP.id}")
                    openScreen(hideStories = false) // Fallback to target screen
                },
                ifRight = { swapStory ->
                    _state.update {
                        SwapStoriesFactory.createStoriesState(
                            swapStory = swapStory,
                            onStoriesClose = ::openScreen,
                        )
                    }
                },
            )
        }
    }
}
