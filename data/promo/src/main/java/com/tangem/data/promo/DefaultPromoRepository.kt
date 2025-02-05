package com.tangem.data.promo

import com.tangem.data.promo.converters.StoryContentResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SHOW_SWAP_STORIES_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.promo.PromoStoriesStore
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.StoryContent
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

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

    override suspend fun getStoryById(id: String): StoryContent = withContext(dispatchers.io) {
        val storedPromo = promoStoriesStore.getSyncOrNull(storyId = id)
        // Get last stored promo by id if possible or get from network
        val story = if (storedPromo == null) {
            val storyContent = tangemApi.getStoryById(
                storyId = id,
                language = SupportedLanguages.getCurrentSupportedLanguageCode(),
            ).getOrThrow()
            promoStoriesStore.store(storyId = id, item = storyContent)
            storyContent
        } else {
            storedPromo
        }

        storyContentConverter.convert(story)
    }

    override fun isReadyToShowSwapStories(): Flow<Boolean> {
        return appPreferencesStore.get(SHOULD_SHOW_SWAP_STORIES_KEY, true)
    }

    override suspend fun isReadyToShowSwapStoriesSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(SHOULD_SHOW_SWAP_STORIES_KEY, true)
    }

    override suspend fun setNeverToShowSwapStories() {
        appPreferencesStore.store(
            key = SHOULD_SHOW_SWAP_STORIES_KEY,
            value = false,
        )
    }
}