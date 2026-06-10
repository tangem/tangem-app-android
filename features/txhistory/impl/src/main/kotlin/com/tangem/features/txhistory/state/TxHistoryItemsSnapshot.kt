package com.tangem.features.txhistory.state

import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Snapshot of transaction history items emitted by [TxHistoryListManager]. Wraps either the
 * primary or legacy item list so that one [Flow] can carry both pipelines, with the active
 * variant chosen via the design feature toggle.
 */
internal sealed interface TxHistoryItemsSnapshot {

    data class Items(val items: ImmutableList<TxHistoryItemsUM.TxHistoryItemUM>) : TxHistoryItemsSnapshot

    data class LegacyItems(val items: ImmutableList<TxHistoryUM.TxHistoryItemUM>) : TxHistoryItemsSnapshot
}