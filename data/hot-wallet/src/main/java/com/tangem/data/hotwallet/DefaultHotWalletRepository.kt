package com.tangem.data.hotwallet

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

internal class DefaultHotWalletRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : HotWalletRepository {

    private val firstTopUpDetectedThisSession = ConcurrentHashMap<String, Unit>()

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    override fun isWalletCreationSupported(): Boolean {
        return BuildConfig.DEBUG || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    override fun getLeastSupportedAndroidVersionName(): String = "Android 10"

    override fun accessCodeSkipped(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(storePreferencesKeys.accessCodeSkippedStatesKey)
        .map { it[userWalletId.stringValue] == true }

    override suspend fun setAccessCodeSkipped(userWalletId: UserWalletId, skipped: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = storePreferencesKeys.accessCodeSkippedStatesKey,
                value = mutablePreferences.getObjectMap<Boolean>(storePreferencesKeys.accessCodeSkippedStatesKey)
                    .plus(userWalletId.stringValue to skipped),
            )
        }
    }

    override fun shouldShowUpgradeBanner(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(storePreferencesKeys.shouldShowUpgradeBannerKey)
        .map { it[userWalletId.stringValue] == true }

    override suspend fun setShouldShowUpgradeBanner(userWalletId: UserWalletId, shouldShow: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = storePreferencesKeys.shouldShowUpgradeBannerKey,
                value = mutablePreferences.getObjectMap<Boolean>(storePreferencesKeys.shouldShowUpgradeBannerKey)
                    .plus(userWalletId.stringValue to shouldShow),
            )
        }
    }

    override fun upgradeBannerClosureTimestamp(userWalletId: UserWalletId): Flow<Long?> = appPreferencesStore
        .getObjectMap<Long>(storePreferencesKeys.upgradeBannerClosureTimestampKey)
        .map { it[userWalletId.stringValue] }

    override suspend fun setUpgradeBannerClosureTimestamp(userWalletId: UserWalletId, timestamp: Long?) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = storePreferencesKeys.upgradeBannerClosureTimestampKey,
                value = mutablePreferences.getObjectMap<Long>(storePreferencesKeys.upgradeBannerClosureTimestampKey)
                    .plus(userWalletId.stringValue to timestamp),
            )
        }
    }

    override suspend fun getWalletCreationTimestamp(userWalletId: UserWalletId): Long? {
        return appPreferencesStore
            .getObjectMapSync<Long>(storePreferencesKeys.walletCreationTimestampKey)[userWalletId.stringValue]
    }

    override suspend fun setWalletCreationTimestamp(userWalletId: UserWalletId, timestamp: Long) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = storePreferencesKeys.walletCreationTimestampKey,
                value = mutablePreferences.getObjectMap<Long>(storePreferencesKeys.walletCreationTimestampKey)
                    .plus(userWalletId.stringValue to timestamp),
            )
        }
    }

    override suspend fun hasHadFirstTopUp(userWalletId: UserWalletId): Boolean {
        return appPreferencesStore
            .getObjectMapSync<Boolean>(storePreferencesKeys.hasHadFirstTopUpKey)[userWalletId.stringValue] == true
    }

    override suspend fun setHasHadFirstTopUp(userWalletId: UserWalletId, hasTopUp: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = storePreferencesKeys.hasHadFirstTopUpKey,
                value = mutablePreferences.getObjectMap<Boolean>(storePreferencesKeys.hasHadFirstTopUpKey)
                    .plus(userWalletId.stringValue to hasTopUp),
            )
        }
    }

    override fun isFirstTopUpDetectedThisSession(userWalletId: UserWalletId): Boolean {
        return firstTopUpDetectedThisSession.containsKey(userWalletId.stringValue)
    }

    override fun markFirstTopUpDetectedThisSession(userWalletId: UserWalletId) {
        firstTopUpDetectedThisSession[userWalletId.stringValue] = Unit
    }
}