package com.tangem.tap.network.exchangeServices

import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.features.demo.isDemoCard

/**
[REDACTED_AUTHOR]
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

    override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean {
        val card = scanResponse.card

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