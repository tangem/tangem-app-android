package com.tangem.data.promo

import com.tangem.data.promo.converters.StoryContentResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowStoriesKey
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.promo.PromoStoriesStore
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.promo.models.StoryContent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class DefaultPromoRepository(
    private val tangemApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val promoStoriesStore: PromoStoriesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val referralRepository: ReferralRepository,
) : PromoRepository {

    private val storyContentConverter = StoryContentResponseConverter()

    override fun isReadyToShowWalletPromo(userWalletId: UserWalletId, promoId: PromoId): Flow<Boolean> {
        return appPreferencesStore.get(
            key = PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name),
            default = true,
        ).map { shouldShow ->
            if (promoId == PromoId.Referral) {
                runCatching {
                    !referralRepository.isReferralParticipant(userWalletId) && shouldShow
                }.getOrDefault(false)
            } else {
                shouldShow
            }
        }
    }

    override fun isReadyToShowTokenPromo(promoId: PromoId): Flow<Boolean> {
        return if (promoId == PromoId.Referral) {
            flowOf(false)
        } else {
            appPreferencesStore.get(
                PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name),
                default = false,
            )
        }
    }

    override suspend fun setNeverToShowWalletPromo(promoId: PromoId) {
        appPreferencesStore.store(PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name), false)
    }

    override suspend fun setNeverToShowTokenPromo(promoId: PromoId) {
        appPreferencesStore.store(PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name), false)
    }

    override suspend fun isMarketsStakingNotificationHideClicked(): Flow<Boolean> {
        return appPreferencesStore.get(
            key = PreferencesKeys.MARKETS_STAKING_NOTIFICATION_HIDE_CLICKED_KEY,
            default = false,
        )
    }

    override suspend fun setMarketsStakingNotificationHideClicked() {
        appPreferencesStore.store(
            key = PreferencesKeys.MARKETS_STAKING_NOTIFICATION_HIDE_CLICKED_KEY,
            value = true,
        )
    }

    override fun getStoryById(id: String): Flow<StoryContent?> = isReadyToShowStories(id).mapLatest {
        getStoryByIdSync(id = id, refresh = false)
    }

    override suspend fun getStoryByIdSync(id: String, refresh: Boolean): StoryContent? = withContext(dispatchers.io) {
        if (!isReadyToShowStoriesSync(id)) return@withContext null

        val storedPromo = promoStoriesStore.getSyncOrNull(storyId = id)
        // Get last stored promo by id if possible or get from network
        val story = if (storedPromo == null && refresh) {
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