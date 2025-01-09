package com.tangem.data.promo

import com.tangem.data.promo.converters.PromoResponseConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SHOW_RING_PROMO_KEY
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoBanner
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class DefaultPromoRepository(
    private val tangemApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoRepository {

    private val promoResponseConverter = PromoResponseConverter()
    override suspend fun getChangellyPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoResponseConverter.convert(
                tangemApi.getPromotionInfo(CHANGELLY_NAME)
                    .getOrThrow(),
            )
        }.getOrNull()
    }

    override suspend fun getOkxPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoResponseConverter.convert(
                tangemApi.getPromotionInfo(OKX)
                    .getOrThrow(),
            )
        }.getOrNull()
    }

    override suspend fun getRingPromoBanner(): PromoBanner? {
        return runCatching(dispatchers.io) {
            promoResponseConverter.convert(
                tangemApi.getPromotionInfo(RING)
                    .getOrThrow(),
            )
        }.getOrNull()
    }

    override fun isReadyToShowWalletSwapPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY, true)
    }

    override fun isReadyToShowTokenSwapPromo(): Flow<Boolean> {
        return appPreferencesStore.get(IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY, true)
    }

    override suspend fun setNeverToShowWalletSwapPromo() {
        appPreferencesStore.store(
            key = IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY,
            value = false,
        )
    }

    override suspend fun setNeverToShowTokenSwapPromo() {
        appPreferencesStore.store(
            key = IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY,
            value = false,
        )
    }

    override fun isReadyToShowRingPromo(userWalletId: UserWalletId): Flow<Boolean> {
        return combine(
            flow = appPreferencesStore
                .get(key = ADDED_WALLETS_WITH_RING_KEY, default = emptySet())
                .map { userWalletId.stringValue in it },
            flow2 = appPreferencesStore.get(key = SHOULD_SHOW_RING_PROMO_KEY, default = true),
            transform = { isRingAdded, shouldShowRingPromo -> isRingAdded && shouldShowRingPromo },
        )
    }

    override suspend fun setNeverToShowRingPromo() {
        appPreferencesStore.store(key = SHOULD_SHOW_RING_PROMO_KEY, value = false)
    }

    private companion object {
        private const val CHANGELLY_NAME = "changelly"
        private const val OKX = "okx"
        private const val RING = "ring"
    }
}