package com.tangem.data.pay.store

import androidx.datastore.core.DataStore
import com.tangem.data.pay.util.CashbackSummaryDMConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.spend.datasource.pay.store.entity.CashbackSummaryDM
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class CashbackSummaryStore(private val dataStore: DataStore<Map<String, CashbackSummaryDM>>) {

    fun get(userWalletId: UserWalletId): Flow<CashbackSummary?> {
        return dataStore.data
            .map { stored -> stored[userWalletId.stringValue]?.let(CashbackSummaryDMConverter::convertBack) }
            .catch { error -> TangemLogger.e("Failed to read the cashback summary cache", error); emit(null) }
            .distinctUntilChanged()
    }

    suspend fun store(userWalletId: UserWalletId, summary: CashbackSummary) {
        val key = userWalletId.stringValue
        val cached = CashbackSummaryDMConverter.convert(summary)
        runSuspendCatching {
            dataStore.updateData { stored -> if (cached == null) stored - key else stored + (key to cached) }
        }.onFailure { TangemLogger.e("Failed to store the cashback summary", it) }
    }

    suspend fun remove(userWalletIds: List<UserWalletId>) {
        val keys = userWalletIds.mapTo(mutableSetOf(), UserWalletId::stringValue)
        runSuspendCatching {
            dataStore.updateData { stored -> stored - keys }
        }.onFailure { TangemLogger.e("Failed to remove the cashback summary cache entries", it) }
    }
}