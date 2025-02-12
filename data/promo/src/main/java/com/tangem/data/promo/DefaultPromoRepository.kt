package com.tangem.data.promo

import com.tangem.data.promo.converters.StoryContentResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowStoriesKey
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.promo.PromoStoriesStore
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.StoryContent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class DefaultPromoRepository(
    private val tangemApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val promoStoriesStore: PromoStoriesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoRepository {

    private val storyContentConverter = StoryContentResponseConverter()

    override fun isReadyToShowWalletSwapPromo(): Flow<Boolean> {
        return flowOf(false) // Use it on new promo action
    }

    override fun isReadyToShowTokenSwapPromo(): Flow<Boolean> {
        return flowOf(false) // Use it on new promo action
    }

    override suspend fun setNeverToShowWalletSwapPromo() {
        // Use it on new promo action
    }

    override suspend fun setNeverToShowTokenSwapPromo() {
        // Use it on new promo action
    }

    override fun getStoryById(id: String): Flow<StoryContent?> = isReadyToShowStories(id).mapLatest {
        getStoryByIdSync(id)
    }

    override suspend fun getStoryByIdSync(id: String): StoryContent? = withContext(dispatchers.io) {
        if (!isReadyToShowStoriesSync(id)) return@withContext null

        val storedPromo = promoStoriesStore.getSyncOrNull(storyId = id)
        // Get last stored promo by id if possible or get from network
        val story = if (storedPromo == null) {
            val storyContent = runCatching {
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
        return appPreferencesStore.get(getShouldShowStoriesKey(storyId), true)
    }

    override suspend fun isReadyToShowStoriesSync(storyId: String): Boolean {
        return appPreferencesStore.getSyncOrDefault(getShouldShowStoriesKey(storyId), true)
    }

    override suspend fun setNeverToShowStories(storyId: String) {
        appPreferencesStore.store(
            key = getShouldShowStoriesKey(storyId),
            value = false,
        )
    }

    private companion object {
        const val STORIES_LOAD_DELAY = 1000L
    }
}