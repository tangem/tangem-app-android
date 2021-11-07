package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.network.moonpay.MoonpayStatus

/**
[REDACTED_AUTHOR]
 */
fun MoonpayStatus.buyIsAllowed(blockchain: Blockchain): Boolean {
    if (blockchain == Blockchain.Unknown || blockchain == Blockchain.BSC) return false

    return isBuyAllowed && availableToBuy.contains(blockchain.currency)
}

fun MoonpayStatus.sellIsAllowed(blockchain: Blockchain): Boolean {
    if (blockchain == Blockchain.Unknown || blockchain == Blockchain.BSC) return false

    return isSellAllowed && availableToSell.contains(blockchain.currency)
}