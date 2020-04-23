package com.tangem

import com.tangem.common.extensions.CardType
import java.util.*

/**
 * Filter that can be used to limit cards that can be interacted with in TangemSdk.
 *
 * @property allowedCardTypes Type of cards that are allowed to be interacted with in TangemSdk.
 */
data class CardFilter(

        var allowedCardTypes: EnumSet<CardType> = EnumSet.allOf(CardType::class.java)
)