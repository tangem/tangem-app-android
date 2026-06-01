package com.tangem.data.stories

import com.tangem.data.stories.converters.StoryContentResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.stories.StoriesStore
import com.tangem.domain.stories.StoriesRepository
import com.tangem.domain.stories.models.StoryContent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class DefaultStoriesRepository(
    private val tangemApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val promoStoriesStore: StoriesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StoriesRepository {

    private val storyContentConverter = StoryContentResponseConverter()

    override fun getStoryById(id: String): Flow<StoryContent?> = isReadyToShowStories(id).mapLatest {
        getStoryByIdSync(id = id, refresh = false)
    }

    override suspend fun getStoryByIdSync(id: String, refresh: Boolean): StoryContent? = withContext(dispatchers.io) {
        if (!isReadyToShowStoriesSync(id)) return@withContext null

        val storedPromo = promoStoriesStore.getSyncOrNull(storyId = id)
        // Get last stored promo by id if possible or get from network
        val story = if (storedPromo == null && refresh) {
            val storyContent = runSuspendCatching {
                // Important to return
                withTimeoutOrNull(STORIES_LOAD_DELAY) {
                    tangemApi.getStoryById(storyId = id).getOrThrow()
                }
            }.getOrNull()
            if (storyContent != null) {
                promoStoriesStore.store(id, storyContent)
            }
            storyContent
        } else {
            storedPromo
        }

        story?.let { storyContentConverter.convert(it) }
    }

    override fun isReadyToShowStories(storyId: String): Flow<Boolean> {
        return appPreferencesStore.get(PreferencesKeys.getShouldShowStoriesKey(storyId), true)
    }

    override suspend fun isReadyToShowStoriesSync(storyId: String): Boolean {
        return appPreferencesStore.getSyncOrDefault(PreferencesKeys.getShouldShowStoriesKey(storyId), true)
    }

    override suspend fun setNeverToShowStories(storyId: String) {
        appPreferencesStore.store(
            key = PreferencesKeys.getShouldShowStoriesKey(storyId),
            value = false,
        )
    }

    private companion object {
        const val STORIES_LOAD_DELAY = 1000L
    }
}