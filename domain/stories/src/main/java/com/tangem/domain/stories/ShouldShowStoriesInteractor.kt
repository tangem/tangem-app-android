package com.tangem.domain.stories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShouldShowStoriesInteractor(
    private val storiesRepository: StoriesRepository,
    private val getStoryContentUseCase: GetStoryContentUseCase,
) {
    operator fun invoke(storyId: String): Flow<Boolean> = getStoryContentUseCase(storyId).map {
        it.getOrNull() != null
    }

    suspend fun invokeSync(storyId: String): Boolean {
        if (!storiesRepository.isReadyToShowStoriesSync(storyId)) return false

        return getStoryContentUseCase.invokeSync(storyId).getOrNull() != null
    }

    suspend fun neverToShow(storyId: String) = storiesRepository.setNeverToShowStories(storyId)
}