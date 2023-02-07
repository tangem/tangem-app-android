package com.tangem.lib.wallet.impl.managers

import com.tangem.lib.wallet.api.WalletManager
import com.tangem.lib.wallet.impl.data.CardTypes
import com.tangem.lib.wallet.impl.data.ScanResponse

class WalletManagersFactory {

    fun createWalletManagerFromScanResponse(scanResponse: ScanResponse): WalletManager {
        return when (getCardType(scanResponse)) {
            CardTypes.TWIN -> TODO()
            CardTypes.MULTI -> MultiWalletManager()
            CardTypes.NOTE -> TODO()
        }
    }

    private fun getCardType(scanResponse: ScanResponse): CardTypes {
        // do logic
        return CardTypes.MULTI
    }
}