package com.tangem.tap.domain

import com.tangem.commands.Card
import java.util.*

class TapWorkarounds(val card: Card) {

    val isStart2Coin: Boolean =
            card.cardData?.issuerName?.toLowerCase(Locale.US) == START_2_COIN_ISSUER

    companion object {
        const val START_2_COIN_ISSUER = "start2coin"
    }
}