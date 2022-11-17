package com.tangem.tap.proxy

import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse

class CardStateHolder {

    val scanResponse: ScanResponse? = null

    fun getActualCard(): Card? {
        return scanResponse?.card
    }
}