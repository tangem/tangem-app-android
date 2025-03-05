package com.tangem.features.txhistory.entity

interface TxHistoryContentUpdateEmitter {
    suspend fun triggerUpdate()
}