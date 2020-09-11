package com.tangem.tap.features.details.redux

import com.tangem.commands.Card
import org.rekotlin.StateType

data class DetailsState(
        val card: Card? = null,
        val cardInfo: CardInfo? = null
) : StateType

data class CardInfo(
        val cardId: String,
        val issuer: String,
        val signedHashes: Int
)
