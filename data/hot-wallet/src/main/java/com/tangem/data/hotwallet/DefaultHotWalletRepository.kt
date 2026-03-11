package com.tangem.data.hotwallet

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

internal class DefaultHotWalletRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : HotWalletRepository {

    private val firstTopUpDetectedThisSession = ConcurrentHashMap<String, Unit>()

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    override fun isWalletCreationSupported(): Boolean {
        return BuildConfig.DEBUG || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    override fun getLeastSupportedAndroidVersionName(): String = "Android 10"

    override fun accessCodeSkipped(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(PreferencesKeys.ACCESS_CODE_SKIPPED_STATES_KEY)
        .map { it[userWalletId.stringValue] == true }

    override suspend fun setAccessCodeSkipped(userWalletId: UserWalletId, skipped: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.ACCESS_CODE_SKIPPED_STATES_KEY,
                value = mutablePreferences.getObjectMap<Boolean>(PreferencesKeys.ACCESS_CODE_SKIPPED_STATES_KEY)
                    .plus(userWalletId.stringValue to skipped),
            )
        }
    }

    override fun shouldShowUpgradeBanner(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(PreferencesKeys.SHOULD_SHOW_UPGRADE_BANNER_KEY)
        .map { it[userWalletId.stringValue] == true }

    override suspend fun setShouldShowUpgradeBanner(userWalletId: UserWalletId, shouldShow: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.SHOULD_SHOW_UPGRADE_BANNER_KEY,
                value = mutablePreferences.getObjectMap<Boolean>(PreferencesKeys.SHOULD_SHOW_UPGRADE_BANNER_KEY)
                    .plus(userWalletId.stringValue to shouldShow),
            )
        }
    }

    override fun upgradeBannerClosureTimestamp(userWalletId: UserWalletId): Flow<Long?> = appPreferencesStore
        .getObjectMap<Long>(PreferencesKeys.UPGRADE_BANNER_CLOSURE_TIMESTAMP_KEY)
        .map { it[userWalletId.stringValue] }

    override suspend fun setUpgradeBannerClosureTimestamp(userWalletId: UserWalletId, timestamp: Long?) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.UPGRADE_BANNER_CLOSURE_TIMESTAMP_KEY,
                value = mutablePreferences.getObjectMap<Long>(PreferencesKeys.UPGRADE_BANNER_CLOSURE_TIMESTAMP_KEY)
                    .plus(userWalletId.stringValue to timestamp),
            )
        }
    }

    override suspend fun getWalletCreationTimestamp(userWalletId: UserWalletId): Long? {
        return appPreferencesStore
            .getObjectMapSync<Long>(PreferencesKeys.WALLET_CREATION_TIMESTAMP_KEY)[userWalletId.stringValue]
    }

    override suspend fun setWalletCreationTimestamp(userWalletId: UserWalletId, timestamp: Long) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.WALLET_CREATION_TIMESTAMP_KEY,
                value = mutablePreferences.getObjectMap<Long>(PreferencesKeys.WALLET_CREATION_TIMESTAMP_KEY)
                    .plus(userWalletId.stringValue to timestamp),
            )
        }
    }

    override suspend fun hasHadFirstTopUp(userWalletId: UserWalletId): Boolean {
        return appPreferencesStore
            .getObjectMapSync<Boolean>(PreferencesKeys.HAS_HAD_FIRST_TOP_UP_KEY)[userWalletId.stringValue] == true
    }

    override suspend fun setHasHadFirstTopUp(userWalletId: UserWalletId, hasTopUp: Boolean) {
        appPreferencesStore.editData { mutablePreferences ->
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.HAS_HAD_FIRST_TOP_UP_KEY,
                value = mutablePreferences.getObjectMap<Boolean>(PreferencesKeys.HAS_HAD_FIRST_TOP_UP_KEY)
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