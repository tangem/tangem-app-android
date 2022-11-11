package com.tangem.tap.network.exchangeServices

import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.wallet.models.Currency

/**
 * Created by Anton Zhilenkov on 10/08/2022.
 */
class CardExchangeRules(
    val cardProvider: () -> CardDTO?,
) : ExchangeRules {

    override fun featureIsSwitchedOn(): Boolean {
        val card = cardProvider() ?: return false

        return !card.isStart2Coin
    }

    override fun isBuyAllowed(): Boolean {
        val card = cardProvider() ?: return false

        return when {
            card.isDemoCard() -> true
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
            card.isDemoCard() -> true
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
