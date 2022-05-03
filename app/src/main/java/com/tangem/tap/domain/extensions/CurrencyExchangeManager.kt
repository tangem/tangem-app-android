package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import com.tangem.tap.network.exchangeServices.CurrencyExchangeStatus
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
fun CurrencyExchangeManager.buyIsAllowed(currency: Currency): Boolean {
    return this.status?.buyIsAllowed(currency) ?: false
}

fun CurrencyExchangeManager.sellIsAllowed(currency: Currency): Boolean {
    return this.status?.sellIsAllowed(currency) ?: false
}

fun CurrencyExchangeStatus.buyIsAllowed(currency: Currency): Boolean {
    if (store.state.globalState.configManager?.config?.isTopUpEnabled == false) return false
    if (!isBuyAllowed) return false

    //TODO: temporary, for the 3.32 release, unlock all buy button
    return true

    return when (currency) {
        is Currency.Blockchain -> {
            val blockchain = currency.blockchain
            when {
                blockchain.isTestnet() -> blockchain.getTestnetTopUpUrl() != null
                blockchain == Blockchain.Unknown -> false
                else -> availableToBuy.contains(currency.currencySymbol)
            }
        }
        is Currency.Token -> false
    }
}

fun CurrencyExchangeStatus.sellIsAllowed(currency: Currency): Boolean {
    if (store.state.globalState.configManager?.config?.isTopUpEnabled == false) return false
    if (!isSellAllowed) return false

    return when (currency) {
        is Currency.Blockchain -> {
            val blockchain = currency.blockchain
            when {
                blockchain.isTestnet() -> false
                blockchain == Blockchain.Unknown || currency.blockchain == Blockchain.BSC -> false
                else -> availableToSell.contains(currency.currencySymbol)
            }
        }
        is Currency.Token -> false
    }
}