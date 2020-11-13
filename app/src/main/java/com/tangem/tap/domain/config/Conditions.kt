package com.tangem.tap.domain.config

import com.tangem.commands.Card
import java.util.*

/**
* [REDACTED_AUTHOR]
 */
interface Condition {
    fun isMet(): Boolean
}

class CardIsStart2CoinCondition(private val card: Card?) : Condition {

    override fun isMet(): Boolean = card?.cardData?.issuerName?.toLowerCase(Locale.US) == "start2coin"
}

class ConditionsFactory {
    companion object {
        fun create(name: String): Condition? {
            return when (name) {
                Feature.payIdIsEnabled -> CardIsStart2CoinCondition(null)
                else -> null
            }
        }
    }
}