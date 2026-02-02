package com.tangem.data.promo

import com.tangem.data.promo.converters.PromoBannerConverter
import com.tangem.data.promo.converters.StoryContentResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowStoriesKey
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.promo.PromoBannerStore
import com.tangem.datasource.local.promo.PromoStoriesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoBanner
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.promo.models.StoryContent
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class DefaultPromoRepository(
    private val tangemApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val promoStoriesStore: PromoStoriesStore,
    private val promoBannerStore: PromoBannerStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val referralRepository: ReferralRepository,
) : PromoRepository {

    private val storyContentConverter = StoryContentResponseConverter()
    private val promoBannerConverter = PromoBannerConverter()

    override fun isReadyToShowWalletPromo(userWalletId: UserWalletId, promoId: PromoId): Flow<Boolean> {
        return appPreferencesStore.get(
            key = PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name),
            default = true,
        )
            .distinctUntilChanged()
            .map { shouldShow ->
                when (promoId) {
                    PromoId.Referral -> runSuspendCatching {
                        !referralRepository.isReferralParticipant(userWalletId) && shouldShow
                    }.getOrDefault(false)
                    PromoId.Sepa -> {
                        val isActive = getSepaPromoBanner()?.isActive == true

                        isActive && shouldShow
                    }
                    PromoId.VisaPresale -> {
                        val isActive = getVisaPromoBanner()?.isActive == true

                        isActive && shouldShow
                    }
                    PromoId.BlackFriday -> {
                        val isActive = getBlackFridayPromoBanner()?.isActive == true

                        isActive && shouldShow
                    }
                    PromoId.OnePlusOne -> {
                        val isActive = getOnePlusOnePromoBanner()?.isActive == true

                        isActive && shouldShow
                    }
                    PromoId.YieldPromo -> {
                        val isActive = getYieldPromoBanner(userWalletId)?.isActive == true

                        isActive && shouldShow
                    }
                }
            }
    }

    override fun isReadyToShowTokenPromo(promoId: PromoId): Flow<Boolean> {
        return when (promoId) {
            PromoId.Referral -> flowOf(false)
            PromoId.Sepa -> flowOf(false)
            PromoId.VisaPresale -> flowOf(false)
            PromoId.BlackFriday -> flowOf(false)
            PromoId.OnePlusOne -> flowOf(false)
            PromoId.YieldPromo -> flowOf(false)
        }
    }

    override suspend fun setNeverToShowWalletPromo(promoId: PromoId) {
        appPreferencesStore.store(PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name), false)
    }

    override suspend fun setNeverToShowTokenPromo(promoId: PromoId) {
        appPreferencesStore.store(PreferencesKeys.getShouldShowPromoKey(promoId = promoId.name), false)
    }

    override fun isMarketsYieldSupplyNotificationHideClicked(): Flow<Boolean> {
        return appPreferencesStore.get(
            key = PreferencesKeys.MARKETS_YIELD_SUPPLY_NOTIFICATION_HIDE_CLICKED_KEY,
            default = false,
        )
    }

    override suspend fun setMarketsYieldSupplyNotificationHideClicked() {
        appPreferencesStore.store(
            key = PreferencesKeys.MARKETS_YIELD_SUPPLY_NOTIFICATION_HIDE_CLICKED_KEY,
            value = true,
        )
    }

    override suspend fun isMoonpayPromoActive(): Boolean {
        val banner = runCatching(dispatchers.io) {
            val response = promoBannerStore.getSyncOrNull(MOONPAY_NAME) ?: run {
                val apiResponse = tangemApi.getPromoBanner(MOONPAY_NAME).getOrThrow()
                promoBannerStore.store(MOONPAY_NAME, apiResponse)
                apiResponse
            }
            promoBannerConverter.convert(response)
        }.getOrNull()
        return banner?.isActive == true
    }

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

    private suspend fun getSepaPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoBannerConverter.convert(
                tangemApi.getPromoBanner(SEPA_NAME).getOrThrow(),
            )
        }.getOrNull()
    }

    private suspend fun getVisaPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoBannerConverter.convert(
                tangemApi.getPromoBanner(VISA_NAME).getOrThrow(),
            )
        }.getOrNull()
    }

    private suspend fun getBlackFridayPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoBannerConverter.convert(
                tangemApi.getPromoBanner(BLACK_FRIDAY_NAME).getOrThrow(),
            )
        }.getOrNull()
    }

    private suspend fun getOnePlusOnePromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoBannerConverter.convert(
                tangemApi.getPromoBanner(ONE_PLUS_ONE_NAME).getOrThrow(),
            )
        }.getOrNull()
    }

    private suspend fun getYieldPromoBanner(userWalletId: UserWalletId): PromoBanner? {
        return runCatching(dispatchers.io) {
            val response = tangemApi.getPromoBannersV2(userWalletId.stringValue).getOrThrow()
            val yieldPromotion = response.promotions.find { it.name == YIELD_PROMO_NAME }
                ?: return@runCatching null
            promoBannerConverter.convert(yieldPromotion)
        }.getOrNull()
    }

    private companion object {
        const val SEPA_NAME = "sepa"
        const val VISA_NAME = "visa-waitlist"
        const val BLACK_FRIDAY_NAME = "black-friday"
        const val MOONPAY_NAME = "moonpay"
        const val ONE_PLUS_ONE_NAME = "one-plus-one"
        const val YIELD_PROMO_NAME = "promo-yield"
        const val STORIES_LOAD_DELAY = 1000L
    }
}