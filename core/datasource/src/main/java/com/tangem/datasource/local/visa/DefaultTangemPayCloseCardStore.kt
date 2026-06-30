package com.tangem.datasource.local.visa

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store

internal class DefaultTangemPayCloseCardStore(
    private val prefs: AppPreferencesStore,
) : TangemPayCloseCardStore {

    override suspend fun storeCloseOrderId(cardId: String, orderId: String) {
        prefs.store(
            key = getCloseKey(cardId),
            value = orderId,
        )
    }

    override suspend fun removeCloseOrderId(cardId: String) {
        prefs.edit { it.remove(getCloseKey(cardId)) }
    }

    override suspend fun getOrderId(cardId: String): String? {
        return prefs.getSyncOrNull(key = getCloseKey(cardId))
    }

    private fun getCloseKey(cardId: String) = stringPreferencesKey("tangem_pay_close_card_$cardId")
}