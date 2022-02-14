package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.network.moonpay.MoonpayStatus
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
fun MoonpayStatus.buyIsAllowed(currency: Currency): Boolean {
    if (store.state.globalState.configManager?.config?.isTopUpEnabled == false) return false
    if (!isBuyAllowed) return false

    return when (currency) {
        is Currency.Blockchain -> {
            val blockchain = currency.blockchain
            when {
                blockchain.isTestnet() -> blockchain.getTestnetTopUpUrl() != null
                blockchain == Blockchain.Unknown || blockchain == Blockchain.BSC -> false
                else -> availableToBuy.contains(currency.currencySymbol)
            }
        }
        is Currency.Token -> false
    }
}

fun MoonpayStatus.sellIsAllowed(currency: Currency): Boolean {
    if (store.state.globalState.configManager?.config?.isTopUpEnabled == false) return false
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