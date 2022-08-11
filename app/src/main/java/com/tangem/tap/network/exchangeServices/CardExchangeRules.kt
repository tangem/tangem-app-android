package com.tangem.tap.network.exchangeServices

import com.tangem.common.card.Card
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.models.Currency

/**
* [REDACTED_AUTHOR]
 */
class CardExchangeRules(
    val cardProvider: () -> Card?,
) : ExchangeRules {

    override fun isBuyAllowed(): Boolean {
        val card = cardProvider() ?: return false

        return when {
            card.isDemoCard() -> false
            card.isStart2Coin -> false
            else -> true
        }
    }

    override fun isSellAllowed(): Boolean {
        val card = cardProvider() ?: return false

        return when {
            card.isDemoCard() -> false
            card.isStart2Coin -> false
            else -> true
        }
    }

    override fun availableForBuy(currency: Currency): Boolean {
        val card = cardProvider() ?: return false

        return when {
            card.isDemoCard() -> false
            card.isStart2Coin -> false
            else -> true
        }
    }

    override fun availableForSell(currency: Currency): Boolean {
        val card = cardProvider() ?: return false

        return when {
            card.isDemoCard() -> false
            card.isStart2Coin -> false
            else -> true
        }
    }
}
