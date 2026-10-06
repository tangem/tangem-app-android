package com.tangem.features.txhistory.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/*
 * Every rule about the network fee of an on-chain transaction: whose fee it is, where its value comes from, and
 * where it surfaces. Shared by the history list, the on-chain details card and the express card — which pulls the
 * fee from its matched on-chain leg — so the three never disagree about what the user paid.
 */

// region Whose fee it is

/**
 * Whether the fee was paid by the counterparty, which drops the fee row: an incoming plain transfer. A non-withdraw
 * [TransactionType.YieldSupply.Send] is a plain transfer too and follows the same rule.
 */
internal fun TxInfo.hasForeignFee(): Boolean = !isOutgoing && (
    type is TransactionType.Transfer ||
        (type as? TransactionType.YieldSupply.Send)?.isYieldSupplyWithdraw == false
    )

/** The amount of a fee-only transaction already shows the fee, so the card does not repeat it as an info row. */
internal fun TxInfo.isFeeOnly(currency: CryptoCurrency): Boolean =
    type is TransactionType.NetworkFee || type is TransactionType.GaslessFee || isFeeOnlyOperation(currency)

/**
 * An unrecognized contract call the wallet made itself, in a coin's own history, that moved no coin: all it cost is
 * the network fee, so the row and the details card show that fee under the plain "Network fee" title. Unlike a
 * decoded [TransactionType.NetworkFee] there is nothing to say the fee was paid *for* — the call was never
 * recognized. An **incoming** one is somebody else's call, a zero-value spam call to the wallet's address being the
 * usual case, and the fee on it is the sender's — so it stays an ordinary operation.
 */
internal fun TxInfo.isFeeOnlyOperation(currency: CryptoCurrency): Boolean = isOutgoing &&
    currency is CryptoCurrency.Coin &&
    amount.isZero() &&
    (type is TransactionType.Operation || type is TransactionType.UnknownOperation)

/**
 * Whether the matched on-chain leg is the one the user paid for. Only a swap viewed from its `from` token qualifies:
 * that leg is the user's own pay-in tx. A payout leg — a swap viewed from its `to` token, or any onramp — is sent by
 * the provider, so its on-chain fee is not the user's and must never surface as "Network fee".
 */
internal val ExpressTx.isPayInLeg: Boolean
    get() = this is ExpressTx.Swap && isOutgoing

// endregion

// region The fee row

/**
 * The network-fee row of a details card, absent while the transaction carries no fee we can name — including a fee
 * that resolves to zero, which no network charges and which the card must not state ([REDACTED_TASK_KEY]).
 */
internal fun TxInfo.feeRows(currency: CryptoCurrency): ImmutableList<TxHistoryDetailsUM.InfoRowUM> =
    listOfNotNull(feeRow(currency)).toImmutableList()

private fun TxInfo.feeRow(currency: CryptoCurrency): TxHistoryDetailsUM.InfoRowUM? {
    val value = gaslessFeeText(currency) ?: nativeFeeText() ?: return null
    return TxHistoryDetailsUM.InfoRowUM(
        label = resourceReference(R.string.common_network_fee_title),
        value = stringReference(value),
        isValueHideable = true,
    )
}

/**
 * A gaslessly sent transaction was paid for with a transfer of [currency] itself, and the native fee in [TxInfo.fee]
 * was paid by the relayer instead — so the gasless leg, when the group carries one, is the fee the user really paid.
 */
private fun TxInfo.gaslessFeeText(currency: CryptoCurrency): String? {
    val gaslessFee = (this as? TxInfo.MultiTransfer)?.gaslessFee ?: return null
    val value = gaslessFee.amount.takeIf { !it.isZero() } ?: return null
    return value.format { crypto(cryptoCurrency = currency, ignoreSymbolPosition = true) }
}

private fun TxInfo.nativeFeeText(): String? {
    val fee = fee ?: return null
    val value = fee.value?.takeIf { !it.isZero() } ?: return null
    return value.format { crypto(symbol = fee.currencySymbol, decimals = fee.decimals, ignoreSymbolPosition = true) }
}

// endregion

// region The fee as the row's own amount

/** The value a row shows: a fee-only transaction moved no coin of its own, so it shows the fee it paid. */
internal fun TxInfo.amountOrFee(currency: CryptoCurrency): BigDecimal =
    if (showsFeeAsAmount(currency)) fee?.value ?: amount else amount

/** The same substitution for the details card, which renders the value as a signed [TextReference]. */
internal fun TxInfo.feeAmountText(currency: CryptoCurrency): TextReference? {
    if (!showsFeeAsAmount(currency)) return null
    val feeValue = fee?.value ?: return null
    val formatted = feeValue.format { crypto(cryptoCurrency = currency, ignoreSymbolPosition = true) }
    val prefix = if (status is TxInfo.TransactionStatus.Failed) "" else "${StringsSigns.MINUS} "
    return stringReference((prefix + formatted).trim())
}

private fun TxInfo.showsFeeAsAmount(currency: CryptoCurrency): Boolean =
    type is TransactionType.NetworkFee || isFeeOnlyOperation(currency)

// endregion