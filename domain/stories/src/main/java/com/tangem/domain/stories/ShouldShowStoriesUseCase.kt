package com.tangem.domain.stories

import kotlinx.coroutines.flow.Flow

class ShouldShowStoriesUseCase(private val storiesRepository: StoriesRepository) {
    operator fun invoke(storyId: String): Flow<Boolean> = storiesRepository.isReadyToShowStories(storyId)
    suspend fun invokeSync(storyId: String): Boolean = storiesRepository.isReadyToShowStoriesSync(storyId)

    suspend fun neverToShow(storyId: String) = storiesRepository.setNeverToShowStories(storyId)
}