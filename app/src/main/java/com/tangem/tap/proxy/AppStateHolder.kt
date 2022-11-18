package com.tangem.tap.proxy

import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.features.wallet.redux.WalletState

class AppStateHolder {

    val scanResponse: ScanResponse? = null
    val walletState: WalletState? = null

    fun getActualCard(): Card? {
        return scanResponse?.card
    }
}