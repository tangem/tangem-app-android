package com.tangem.domain.models.currency

fun CryptoCurrency.Token.yieldSupplyKey(): String {
    return "${network.backendId}_$contractAddress"
}

fun CryptoCurrencyStatus.yieldSupplyNotAllAmountSupplied(): Boolean {
    if (this.currency !is CryptoCurrency.Token) return false

    val supplyStatus = this.value.yieldSupplyStatus
    if (supplyStatus?.isActive != true) return false

    val protocolBalance = supplyStatus.effectiveProtocolBalance
    val amount = this.value.amount

    return if (protocolBalance != null && amount != null) {
        amount > protocolBalance
    } else {
        false
    }
}