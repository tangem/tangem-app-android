package com.tangem.datasource.local.visa

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.visa.entity.TangemPayPendingOrderDM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DefaultTangemPayPendingOrdersStore(
    private val prefs: AppPreferencesStore,
) : TangemPayPendingOrdersStore {

    override fun getAllFlow(): Flow<List<TangemPayPendingOrderDM>> {
        return prefs.getObjectMap<TangemPayPendingOrderDM>(KEY)
            .map { orders -> orders.values.toList() }
            .distinctUntilChanged()
    }

    override suspend fun getAll(): List<TangemPayPendingOrderDM> {
        return prefs.getObjectMapSync<TangemPayPendingOrderDM>(KEY).values.toList()
    }

    override suspend fun getByCard(cardId: String): List<TangemPayPendingOrderDM> {
        return prefs.getObjectMapSync<TangemPayPendingOrderDM>(KEY).values.filter { it.cardId == cardId }
    }

    override suspend fun save(order: TangemPayPendingOrderDM) {
        prefs.editData { mutablePrefs ->
            val current = mutablePrefs.getObjectMap<TangemPayPendingOrderDM>(KEY)
            mutablePrefs.setObjectMap(KEY, current + (order.orderId to order))
        }
    }

    override suspend fun remove(orderId: String) {
        prefs.editData { mutablePrefs ->
            val current = mutablePrefs.getObjectMap<TangemPayPendingOrderDM>(KEY)
            if (current.containsKey(orderId)) {
                mutablePrefs.setObjectMap(KEY, current - orderId)
            }
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("tangem_pay_pending_orders")
    }
}