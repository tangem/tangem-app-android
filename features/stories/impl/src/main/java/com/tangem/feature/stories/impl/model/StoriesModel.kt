package com.tangem.feature.stories.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.feature.stories.api.StoriesComponent
import com.tangem.feature.stories.api.StoriesUM
import com.tangem.feature.stories.impl.StoriesSlideConfigs
import com.tangem.feature.stories.impl.analytics.StoriesEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class StoriesModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val shouldShowStoriesUseCase: ShouldShowStoriesUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    val state: StateFlow<StoriesUM>
        field = MutableStateFlow<StoriesUM>(value = StoriesUM.Empty)

    private val params = paramsContainer.require<StoriesComponent.Params>()

    init {
        initStories()
    }

    private fun openScreen(hideStories: Boolean = true) {
        modelScope.launch {
            if (hideStories) {
                shouldShowStoriesUseCase.neverToShow(params.storyId)
            }
            router.pop()
            params.nextScreen?.let { router.push(it) }
        }
    }

    private fun initStories() {
        modelScope.launch {
            getStoryContentUseCase.invokeSync(params.storyId).fold(
                ifLeft = {
                    TangemLogger.e("Unable to load stories for ${params.storyId}")
                    openScreen(hideStories = false)
                },
                ifRight = { story ->
                    if (story == null) {
                        openScreen(hideStories = false)
                    } else {
                        val slides = StoriesSlideConfigs.getSlides(params.storyId)
                        val imageUrls = story.getImageUrls()
                        val configs = slides.zip(imageUrls) { slide, imageUrl ->
                            StoriesUM.Content.Config(
                                imageUrl = imageUrl,
                                title = resourceReference(slide.titleResId),
                                subtitle = resourceReference(slide.subtitleResId),
                            )
                        }.toImmutableList()

                        if (configs.isEmpty()) {
                            openScreen(hideStories = false)
                            return@launch
                        }

                        state.update {
                            StoriesUM.Content(
                                stories = configs,
                                onClose = { watchCount ->
                                    analyticsEventHandler.send(
                                        StoriesEvents.SwapStories(
                                            source = params.screenSource,
                                            watchCount = watchCount.toString(),
                                        ),
                                    )
                                    openScreen()
                                },
                            )
                        }
                    }
                },
            )
        }
    }
}