package com.tangem.data.pay.store

import com.tangem.data.pay.entity.BankOfframpRecentUsage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.spend.datasource.pay.store.TangemPayPreferencesKeys

internal class DefaultBankOfframpRecentsStore(
    private val prefs: AppPreferencesStore,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : BankOfframpRecentsStore {

    override suspend fun markUsed(userWalletId: UserWalletId, bankAccountId: String) {
        val key = TangemPayPreferencesKeys.bankOfframpRecents(userWalletId)
        prefs.editData { preferences ->
            val current = preferences.getObjectMap<BankOfframpRecentUsage>(key)
            preferences.setObjectMap(
                key = key,
                value = current + (bankAccountId to BankOfframpRecentUsage(usedAtMillis = currentTimeMillis())),
            )
        }
    }

    override suspend fun getUsage(userWalletId: UserWalletId): Map<String, Long> {
        val key = TangemPayPreferencesKeys.bankOfframpRecents(userWalletId)
        return prefs.getObjectMapSync<BankOfframpRecentUsage>(key).mapValues { it.value.usedAtMillis }
    }
}