package com.tangem.features.tangempay.utils

import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus

internal data class TangemPayTxHistoryState(
    val status: PaginationStatus<*> = PaginationStatus.None,
    val uiBatches: List<Batch<Int, List<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM>>> = listOf(),
    val isEmpty: Boolean = false,
)