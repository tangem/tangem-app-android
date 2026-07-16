package com.tangem.features.txhistory.state

import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Snapshot of transaction history items emitted by [TxHistoryListManager].
 */
internal sealed interface TxHistoryItemsSnapshot {

    data class Items(val items: ImmutableList<TxHistoryItemsUM.TxHistoryItemUM>) : TxHistoryItemsSnapshot
}