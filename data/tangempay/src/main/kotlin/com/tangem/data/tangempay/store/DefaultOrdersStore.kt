package com.tangem.data.tangempay.store

import com.tangem.data.tangempay.entity.OrderRefDM
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getJsonOrNull
import com.tangem.datasource.local.preferences.utils.getJsonSyncOrNull
import com.tangem.datasource.local.preferences.utils.setJson
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.order.OrdersStore
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal class DefaultOrdersStore(
    private val prefs: AppPreferencesStore,
    private val spendPreferencesKeys: SpendPreferencesKeys,
    private val json: Json,
) : OrdersStore {

    private val serializer = ListSerializer(OrderRefDM.serializer())

    override suspend fun save(order: OrderRef) {
        val record = order.toDataModel()
        edit(order.userWalletId) { current -> current.filterNot { it.orderId == record.orderId } + record }
    }

    override suspend fun remove(order: OrderRef) {
        edit(order.userWalletId) { current -> current.filterNot { it.orderId == order.id } }
    }

    override suspend fun getAll(userWalletId: UserWalletId): List<OrderRef> {
        return prefs.getJsonSyncOrNull(spendPreferencesKeys.orderRefs(userWalletId), json, serializer)
            .orEmpty()
            .map { it.toDomainModel(userWalletId) }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        val key = spendPreferencesKeys.orderRefs(userWalletId)
        prefs.editData { preferences -> preferences.remove(key) }
    }

    private suspend fun edit(userWalletId: UserWalletId, transform: (List<OrderRefDM>) -> List<OrderRefDM>) {
        val key = spendPreferencesKeys.orderRefs(userWalletId)
        prefs.editData { preferences ->
            val current = preferences.getJsonOrNull(key, json, serializer).orEmpty()
            preferences.setJson(key = key, json = json, serializer = serializer, value = transform(current))
        }
    }

    private fun OrderRef.toDataModel() = OrderRefDM(orderId = id)

    private fun OrderRefDM.toDomainModel(userWalletId: UserWalletId) = OrderRef(
        userWalletId = userWalletId,
        id = orderId,
    )
}