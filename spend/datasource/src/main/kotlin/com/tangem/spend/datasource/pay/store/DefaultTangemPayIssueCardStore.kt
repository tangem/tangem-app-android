package com.tangem.spend.datasource.pay.store

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getJsonOrNull
import com.tangem.datasource.local.preferences.utils.getJsonSyncOrNull
import com.tangem.datasource.local.preferences.utils.setJson
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class DefaultTangemPayIssueCardStore(
    private val prefs: AppPreferencesStore,
    private val json: Json,
) : TangemPayIssueCardStore {

    override suspend fun addIssueOrderId(userWalletId: UserWalletId, orderId: String) {
        prefs.editData { preferences ->
            val key = getKey(userWalletId)
            val current = preferences.getJsonOrNull(key, json, SERIALIZER).orEmpty()
            if (orderId !in current) {
                preferences.setJson(key = key, json = json, serializer = SERIALIZER, value = current + orderId)
            }
        }
    }

    override suspend fun getIssueOrderIds(userWalletId: UserWalletId): List<String> {
        return prefs.getJsonSyncOrNull(getKey(userWalletId), json, SERIALIZER).orEmpty()
    }

    override suspend fun removeIssueOrderId(userWalletId: UserWalletId, orderId: String) {
        prefs.editData { preferences ->
            val key = getKey(userWalletId)
            val current = preferences.getJsonOrNull(key, json, SERIALIZER).orEmpty()
            if (orderId in current) {
                preferences.setJson(key = key, json = json, serializer = SERIALIZER, value = current - orderId)
            }
        }
    }

    private fun getKey(userWalletId: UserWalletId) =
        stringPreferencesKey("tangem_pay_issue_card_orders_${userWalletId.stringValue}")

    private companion object {
        val SERIALIZER = ListSerializer(String.serializer())
    }
}