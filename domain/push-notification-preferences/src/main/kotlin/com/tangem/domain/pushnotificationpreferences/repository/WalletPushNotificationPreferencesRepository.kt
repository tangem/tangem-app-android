package com.tangem.domain.pushnotificationpreferences.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import kotlinx.coroutines.flow.Flow

/** Per-wallet push notification preferences. In-memory cache, not persisted. */
interface WalletPushNotificationPreferencesRepository {

    /** Warms up the cache for [userWalletId]. No-op if already cached. */
    suspend fun preload(userWalletId: UserWalletId)

    fun observePreferences(userWalletId: UserWalletId): Flow<WalletPushNotificationPreferences>

    /** Updates a single [category]; full-replace PUT under the hood. On failure cache is untouched. */
    suspend fun updatePreference(
        userWalletId: UserWalletId,
        category: PushNotificationCategory,
        isEnabled: Boolean,
    ): Either<Throwable, Unit>
}