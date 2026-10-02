package com.tangem.data.pay.store

import com.tangem.data.pay.entity.BankOfframpRecentUsage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getJsonOrNull
import com.tangem.datasource.local.preferences.utils.getJsonSyncOrNull
import com.tangem.datasource.local.preferences.utils.setJson
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class DefaultBankOfframpRecentsStore(
    private val prefs: AppPreferencesStore,
    private val spendPreferencesKeys: SpendPreferencesKeys,
    private val json: Json,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : BankOfframpRecentsStore {

    private val usageSerializer = MapSerializer(String.serializer(), BankOfframpRecentUsage.serializer())

    override suspend fun markUsed(userWalletId: UserWalletId, bankAccountId: String) {
        val key = spendPreferencesKeys.bankOfframpRecents(userWalletId)
        prefs.editData { preferences ->
            val current = preferences.getJsonOrNull(key, json, usageSerializer).orEmpty()
            val updated = current + (bankAccountId to BankOfframpRecentUsage(usedAtMillis = currentTimeMillis()))

            preferences.setJson(key = key, json = json, serializer = usageSerializer, value = updated)
        }
    }

    override suspend fun getUsage(userWalletId: UserWalletId): Map<String, Long> {
        val key = spendPreferencesKeys.bankOfframpRecents(userWalletId)

        return prefs.getJsonSyncOrNull(key, json, usageSerializer).orEmpty().mapValues { it.value.usedAtMillis }
    }
}