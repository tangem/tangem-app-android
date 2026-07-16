package com.tangem.datasource.local.visa

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.storeObjectList
import com.tangem.domain.models.wallet.UserWalletId

internal class DefaultTangemPayIssueCardStore(
    private val prefs: AppPreferencesStore,
) : TangemPayIssueCardStore {

    override suspend fun addIssueOrderId(userWalletId: UserWalletId, orderId: String) {
        val current = prefs.getObjectListSync<String>(getKey(userWalletId))
        if (orderId !in current) {
            prefs.storeObjectList(key = getKey(userWalletId), value = current + orderId)
        }
    }

    override suspend fun getIssueOrderIds(userWalletId: UserWalletId): List<String> {
        return prefs.getObjectListSync(getKey(userWalletId))
    }

    override suspend fun removeIssueOrderId(userWalletId: UserWalletId, orderId: String) {
        val current = prefs.getObjectListSync<String>(getKey(userWalletId))
        if (orderId in current) {
            prefs.storeObjectList(key = getKey(userWalletId), value = current - orderId)
        }
    }

    private fun getKey(userWalletId: UserWalletId) =
        stringPreferencesKey("tangem_pay_issue_card_orders_${userWalletId.stringValue}")
}