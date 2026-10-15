package com.tangem.domain.transaction.models.tron

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import java.math.BigDecimal

/**
 * TRX is taken without quoting the gasless fee when it covers the native fee and the account's own energy
 * covers the transfer: such a fee is close to zero, and the decision needs neither rates nor the gasless service.
 */
fun TransactionFee.isPaidByOwnTronEnergy(nativeStatus: CryptoCurrencyStatus): Boolean =
    nativeStatus.canPayNativeFee(this) && isCoveredByOwnTronEnergy()

/** Once the gasless fee is quoted, TRX is taken when it covers the native fee and costs no more in fiat. */
fun TransactionFee.isPreferredOverTronGasless(
    gaslessFee: TransactionFee,
    nativeStatus: CryptoCurrencyStatus,
    gaslessTokenStatus: CryptoCurrencyStatus,
): Boolean = nativeStatus.canPayNativeFee(this) &&
    isCheaperInFiatThan(gaslessFee = gaslessFee, nativeStatus = nativeStatus, gaslessTokenStatus = gaslessTokenStatus)

/** Only a coin pays: the fee-paid status falls back to the sent token when the network coin is unresolved. */
fun CryptoCurrencyStatus.canPayNativeFee(fee: TransactionFee): Boolean {
    if (currency !is CryptoCurrency.Coin) return false
    val feeValue = fee.normal.amount.value ?: return false
    val balance = value.amount ?: return false
    return balance >= feeValue
}

/** A fee with no estimated energy is not treated as covered: a token transfer always needs energy. */
internal fun TransactionFee.isCoveredByOwnTronEnergy(): Boolean {
    val tronFee = normal as? Fee.Tron ?: return false
    return tronFee.feeEnergy > 0 && tronFee.remainingEnergy >= tronFee.feeEnergy
}

/**
 * The SDK's native Tron fee already nets out the account's own energy, partial energy included. Without a
 * positive rate for either currency the fees are not comparable and the native fee is not preferred.
 */
internal fun TransactionFee.isCheaperInFiatThan(
    gaslessFee: TransactionFee,
    nativeStatus: CryptoCurrencyStatus,
    gaslessTokenStatus: CryptoCurrencyStatus,
): Boolean {
    val nativeFiat = fiatValue(nativeStatus) ?: return false
    val gaslessFiat = gaslessFee.fiatValue(gaslessTokenStatus) ?: return false
    return nativeFiat <= gaslessFiat
}

private fun TransactionFee.fiatValue(status: CryptoCurrencyStatus): BigDecimal? {
    val feeValue = normal.amount.value ?: return null
    val rate = status.value.fiatRate?.takeIf { it.signum() > 0 } ?: return null
    return feeValue * rate
}