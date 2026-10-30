package com.tangem.domain.stories

class ShouldShowStoriesInteractor(
    private val storiesRepository: StoriesRepository,
    private val getStoryContentUseCase: GetStoryContentUseCase,
) {
    suspend fun invokeSync(storyId: String): Boolean {
        if (!storiesRepository.isReadyToShowStoriesSync(storyId)) return false

        return getStoryContentUseCase.invokeSync(storyId).getOrNull() != null
    }

    suspend fun neverToShow(storyId: String) = storiesRepository.setNeverToShowStories(storyId)
}