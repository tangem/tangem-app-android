package com.tangem.features.tangempay.multichain

import com.tangem.common.ui.extensions.iconResId
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Pure display data for a single network row in the Choose-network bottom sheet, derived from a
 * [PaymentNetworkStatus]. See [toRowData].
 *
 * @property id network identity, used as a stable list/compose key.
 * @property name human-readable network name, e.g. "Polygon".
 * @property tokensLabel comma-joined token symbols available on this network, e.g. "USDC, USDT".
 * @property iconResId network icon drawable resource.
 */
internal data class PaymentNetworkRowData(
    val id: String,
    val name: String,
    val tokensLabel: String,
    val iconResId: Int,
)

/**
 * Whether this network belongs to the **Other ways** section of the Choose-network sheet: info-only networks
 * with no contract, which cannot be received on directly. Everything else is a **Fast way** network.
 *
 * Single source of truth for the split — the sheet's sections and the analytics event fired on a row tap
 * must not drift apart.
 */
internal fun PaymentNetworkStatus.isOtherWay(): Boolean = this is PaymentNetworkStatus.Disabled

/**
 * The currencies carried by this network status. A [PaymentNetworkStatus.NotIssued] network carries none:
 * its contract does not exist yet, so there are no contract addresses to build currencies from.
 */
internal fun PaymentNetworkStatus.currencies(): List<CryptoCurrency> = when (this) {
    is PaymentNetworkStatus.Available -> cryptoCurrencyStatuses.map { it.currency }
    is PaymentNetworkStatus.NotIssued -> emptyList()
    is PaymentNetworkStatus.Disabled -> cryptoCurrencies
}

/**
 * The subset of [currencies] the user is offered to receive: tokens Tangem has a catalogue entry (raw id) for.
 *
 * A payment account may also carry a token that is not in the catalogue — the backend's internal settlement
 * stablecoin, for one. It has no name or icon to show (it would render as an anonymous placeholder), and the
 * account is not meant to receive it, so it is hidden from the whole receive flow: the network row's token
 * label and the Receive sheet alike.
 */
internal fun PaymentNetworkStatus.receivableCurrencies(): List<CryptoCurrency> = currencies()
    .filter { it.id.rawCurrencyId != null }

/**
 * Maps this status to row display data: network identity (id, name, icon) from [PaymentNetworkStatus.network],
 * token label from the contained currencies' symbols. A [PaymentNetworkStatus.NotIssued] network carries no
 * currencies, so its label is the fixed set of payment stablecoins the account will hold once issued.
 *
 * `null` only for a [PaymentNetworkStatus.Available] network without currencies — it is issued, yet there is
 * nothing to receive on it. [PaymentNetworkStatus.NotIssued] and [PaymentNetworkStatus.Disabled] rows are kept
 * regardless: the row itself is still actionable (issue on demand) or informational.
 */
internal fun PaymentNetworkStatus.toRowData(): PaymentNetworkRowData? {
    val currencies = receivableCurrencies()
    if (currencies.isEmpty() && this is PaymentNetworkStatus.Available) return null
    return PaymentNetworkRowData(
        id = network.rawId,
        name = network.name,
        tokensLabel = when (this) {
            is PaymentNetworkStatus.NotIssued -> NOT_ISSUED_TOKENS_LABEL
            else -> currencies.joinToString(separator = ", ") { it.symbol }
        },
        iconResId = network.iconResId,
    )
}

private const val NOT_ISSUED_TOKENS_LABEL = "USDC, USDT"