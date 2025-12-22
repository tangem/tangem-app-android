package com.tangem.domain.models.currency

import java.math.BigDecimal

fun CryptoCurrency.Token.yieldSupplyKey(): String {
    return "${network.backendId}_$contractAddress"
}

fun CryptoCurrencyStatus.hasNotSuppliedAmount(): Boolean {
    val notSupplied = notSuppliedCryptoAmountOrNull() ?: return false
    return notSupplied > BigDecimal.ZERO
}

fun CryptoCurrencyStatus.shouldShowNotSuppliedNotification(dustAmount: BigDecimal): Boolean {
    val notSupplied = notSuppliedCryptoAmountOrNull()?.multiply(this.value.fiatRate) ?: return false
    return notSupplied >= dustAmount
}

fun CryptoCurrencyStatus.notSuppliedCryptoAmountOrNull(): BigDecimal? {
    if (this.currency !is CryptoCurrency.Token) return null

    val supplyStatus = this.value.yieldSupplyStatus
    if (supplyStatus?.isActive != true) return null

    val protocolBalance = supplyStatus.effectiveProtocolBalance
    val amount = this.value.amount

    return if (protocolBalance != null && amount != null) {
        amount.minus(protocolBalance)
    } else {
        null
    }
}