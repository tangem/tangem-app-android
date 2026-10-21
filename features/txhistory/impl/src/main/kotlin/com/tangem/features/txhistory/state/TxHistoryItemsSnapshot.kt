package com.tangem.features.txhistory.state

import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import kotlinx.collections.immutable.ImmutableList

internal sealed interface TxHistoryItemsSnapshot {

    data class Items(val items: ImmutableList<TxHistoryItemsUM.TxHistoryItemUM>) : TxHistoryItemsSnapshot
}