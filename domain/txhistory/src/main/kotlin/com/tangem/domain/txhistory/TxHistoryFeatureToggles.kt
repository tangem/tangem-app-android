package com.tangem.domain.txhistory

interface TxHistoryFeatureToggles {

    /** The coin history also loads internal transfers, where a Lending withdrawal pays the coin out */
    val isLendingHistoryEnabled: Boolean
}