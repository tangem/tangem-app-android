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
 * The currencies carried by this network status, regardless of its issuance state.
 */
internal fun PaymentNetworkStatus.currencies(): List<CryptoCurrency> = when (this) {
    is PaymentNetworkStatus.Available -> cryptoCurrencyStatuses.map { it.currency }
    is PaymentNetworkStatus.NotIssued -> cryptoCurrencies
    is PaymentNetworkStatus.Disabled -> cryptoCurrencies
}

/**
 * Maps this status to row display data: network identity (id, name, icon) from [PaymentNetworkStatus.network],
 * token label from the contained currencies' symbols. `null` when the status carries no currencies —
 * a network with nothing to receive has no row.
 */
internal fun PaymentNetworkStatus.toRowData(): PaymentNetworkRowData? {
    val currencies = currencies()
    if (currencies.isEmpty()) return null
    return PaymentNetworkRowData(
        id = network.rawId,
        name = network.name,
        tokensLabel = currencies.joinToString(separator = ", ") { it.symbol },
        iconResId = network.iconResId,
    )
}