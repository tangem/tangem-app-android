package com.tangem.data.hotwallet

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultHotWalletRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : HotWalletRepository {

    override fun accessCodeSkipped(userWalletId: UserWalletId): Flow<Boolean> = appPreferencesStore
        .getObjectMap<Boolean>(PreferencesKeys.ACCESS_CODE_SKIPPED_STATES_KEY)
        .map { it[userWalletId.stringValue] == true }

    override suspend fun setAccessCodeSkipped(userWalletId: UserWalletId, skipped: Boolean) {
        appPreferencesStore.editData {
            it.setObjectMap(
                key = PreferencesKeys.ACCESS_CODE_SKIPPED_STATES_KEY,
                value = it.getObjectMap<Boolean>(PreferencesKeys.ACCESS_CODE_SKIPPED_STATES_KEY)
                    .plus(userWalletId.stringValue to skipped),
            )
        }
    }
}