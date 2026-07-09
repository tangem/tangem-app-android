package com.tangem.domain.txhistory.list

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.TxHistoryInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Reads the unified transaction history for a currency: the on-chain backbone (BSDK / TangemPay / index-backed

 * (wallet, currency) via [Factory].
 */
interface HistoryTxListManager {

    /** The whole history state, newest first — the single source of truth for the UI. */
    val state: StateFlow<HistoryState>

    val historySources: Flow<HistorySources>

    fun reload()

    fun loadMore()

    /** Universal history state: the caller renders exactly one of these. */
    sealed interface HistoryState {
        data object Loading : HistoryState
        data object Unavailable : HistoryState
        data object Empty : HistoryState
        data object Error : HistoryState
        data class Content(
            val items: List<TxHistoryInfo>,
            val isLoadingMore: Boolean,
            val hasMore: Boolean,
        ) : HistoryState

        val isContent: Boolean
            get() = when (this) {
                is Content -> true
                Empty,
                Error,
                Loading,
                Unavailable,
                -> false
            }
    }

    /** How the history starts for a currency: which on-chain backbone exists and whether express is available. */
    data class HistorySources(
        val onChainSource: OnChainSource,
        val isExchangeAvailable: Boolean,
        val isOnrampAvailable: Boolean,
    )

    data class HistoryEnvironment(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val modelScope: CoroutineScope,
    )

    enum class OnChainSource { BSDK, TangemPay, IndexTable }

    interface Factory {
        fun create(
            userWalletId: UserWalletId,
            currency: CryptoCurrency,
            modelScope: CoroutineScope,
        ): HistoryTxListManager
    }
}

/**
 * Reactive stream of a single row tracked by its [TxHistoryInfo.txId], for the in-app details sheet.
 *
 * Seeded with the tapped [item] so the sheet always has an immediate snapshot, then re-emits the matching row from
 * the live merged list as its status changes. The seed also covers rows not present in [state] yet (e.g. a pending
 * tx surfaced from the currency status), which would otherwise never resolve.
 */
fun HistoryTxListManager.txHistoryInfoFlow(item: TxHistoryInfo): Flow<TxHistoryInfo> = state
    .mapNotNull { (it as? HistoryState.Content)?.items }
    .mapNotNull { list -> list.firstOrNull { it.txId == item.txId } }
    .onStart { emit(item) }
    .distinctUntilChanged()