package com.tangem.domain.txhistory

interface TxHistoryFeatureToggles {
    val isSolanaTxHistoryEnabled: Boolean
    val isNewTxHistoryEnabled: Boolean
}