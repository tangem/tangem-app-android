package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerKind
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerStore
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class DefaultPolymarketFastTradingBannerStore(
    private val dataStore: DataStore<Map<String, Map<String, Long>>>,
) : PolymarketFastTradingBannerStore {

    override fun dismissedAt(userWalletId: UserWalletId, kind: PolymarketFastTradingBannerKind): Flow<Instant?> =
        dataStore.data
            .map { stored -> stored[userWalletId.stringValue]?.get(kind.storageKey())?.let(Instant::ofEpochMilli) }
            .distinctUntilChanged()
            .catch { error ->
                TangemLogger.e("Failed to observe the ${kind.name} banner dismissal of $userWalletId", error)
                emit(null)
            }

    override suspend fun dismiss(userWalletId: UserWalletId, kind: PolymarketFastTradingBannerKind, at: Instant) {
        edit(userWalletId) { stored ->
            val walletDismissals = stored[userWalletId.stringValue].orEmpty() + (kind.storageKey() to at.toEpochMilli())
            stored + (userWalletId.stringValue to walletDismissals)
        }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        edit(userWalletId) { stored -> stored - userWalletId.stringValue }
    }

    private suspend fun edit(
        userWalletId: UserWalletId,
        update: (Map<String, Map<String, Long>>) -> Map<String, Map<String, Long>>,
    ) {
        runSuspendCatching { dataStore.updateData(update) }
            .onFailure { TangemLogger.e("Failed to update the banner dismissals of $userWalletId", it) }
    }
}

private fun PolymarketFastTradingBannerKind.storageKey(): String = when (this) {
    PolymarketFastTradingBannerKind.ACTIVATE -> "activate_banner"
    PolymarketFastTradingBannerKind.EXTEND -> "extend_banner"
}