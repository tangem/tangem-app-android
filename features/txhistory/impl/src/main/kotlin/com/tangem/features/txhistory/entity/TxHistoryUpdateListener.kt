package com.tangem.features.txhistory.entity

import kotlinx.coroutines.flow.Flow

internal interface TxHistoryUpdateListener {
    val updates: Flow<Unit>
}