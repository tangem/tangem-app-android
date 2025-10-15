package com.tangem.domain.models.currency

fun CryptoCurrency.Token.yieldSupplyKey(): String {
    return "${network.backendId}_$contractAddress"
}