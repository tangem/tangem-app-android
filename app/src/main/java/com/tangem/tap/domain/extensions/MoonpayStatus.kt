package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.network.moonpay.MoonpayStatus

/**
[REDACTED_AUTHOR]
 */
fun MoonpayStatus.buyIsAllowed(currency: Currency): Boolean {
    if (!isBuyAllowed) return false

    return when (currency) {
        is Currency.Blockchain -> {
            if (currency.blockchain == Blockchain.Unknown || currency.blockchain == Blockchain.BSC) {
                false
            } else {
                availableToBuy.contains(currency.currencySymbol)
            }
        }
        is Currency.Token -> false
    }
}

fun MoonpayStatus.sellIsAllowed(currency: Currency): Boolean {
    if (!isSellAllowed) return false

    return when (currency) {
        is Currency.Blockchain -> {
            if (currency.blockchain == Blockchain.Unknown || currency.blockchain == Blockchain.BSC) {
                false
            } else {
                availableToSell.contains(currency.currencySymbol)
            }
        }
        is Currency.Token -> false
    }
}