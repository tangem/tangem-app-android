package com.tangem.datasource.local.visa

import androidx.datastore.core.DataStore
import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemDM
import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemToDMConverter
import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemToDomainConverter
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import kotlinx.coroutines.flow.first

internal typealias StoredTangemPayTxHistory = Map<String, Map<String, List<TangemPayTxHistoryItemDM>>>

internal class DefaultTangemPayTxHistoryItemsStore(
    private val dataStore: DataStore<StoredTangemPayTxHistory>,
    private val toDMConverter: TangemPayTxHistoryItemToDMConverter,
    private val toDomainConverter: TangemPayTxHistoryItemToDomainConverter,
) : TangemPayTxHistoryItemsStore {

    override suspend fun getSyncOrNull(key: String, cursor: String): List<TangemPayTxHistoryItem>? {
        return dataStore.data.first()[key]?.get(cursor)?.let { toDomainConverter.convertList(it) }
    }

    override suspend fun store(key: String, cursor: String, value: List<TangemPayTxHistoryItem>) {
        val page = toDMConverter.convertList(value)
        dataStore.updateData { stored ->
            val walletPages = stored[key].orEmpty()
            stored + (key to walletPages + (cursor to page))
        }
    }

    override suspend fun remove(key: String) {
        remove(keys = listOf(key))
    }

    override suspend fun remove(keys: List<String>) {
        dataStore.updateData { stored -> stored - keys.toSet() }
    }
}