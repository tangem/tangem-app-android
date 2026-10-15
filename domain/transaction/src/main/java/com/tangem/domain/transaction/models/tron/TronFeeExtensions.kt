package com.tangem.domain.transaction.models.tron

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus

/** A fee with no estimated energy is not treated as covered: a token transfer always needs energy. */
fun TransactionFee.isCoveredByOwnTronEnergy(): Boolean {
    val tronFee = normal as? Fee.Tron ?: return false
    return tronFee.feeEnergy > 0 && tronFee.remainingEnergy >= tronFee.feeEnergy
}

/** Only a coin pays: the fee-paid status falls back to the sent token when the network coin is unresolved. */
fun CryptoCurrencyStatus.canPayNativeFee(fee: TransactionFee): Boolean {
    if (currency !is CryptoCurrency.Coin) return false
    val feeValue = fee.normal.amount.value ?: return false
    val balance = value.amount ?: return false
    return balance >= feeValue
}