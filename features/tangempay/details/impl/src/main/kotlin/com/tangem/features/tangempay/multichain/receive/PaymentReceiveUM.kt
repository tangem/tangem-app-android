package com.tangem.features.tangempay.multichain.receive

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the pay-specific multi-token "Receive assets" bottom sheet, shown for an already-issued
 * (Available) multichain network that can carry more than one token (e.g. USDC and USDT on the same
 * network share a single deposit address).
 *
 * @property warning formatted "Send only {tokens} on {network} network" banner headline.
 * @property tokensOnNetworkLabel formatted "{tokens} on {network} network" heading shown above the address.
 * @property tokens the network's tokens, each with its own icon (with network badge).
 * @property address the deposit address shared by all [tokens] on the network.
 */
internal data class PaymentReceiveUM(
    val warning: TextReference,
    val tokensOnNetworkLabel: TextReference,
    val tokens: ImmutableList<TokenIconUM>,
    val address: String,
    val onCopy: () -> Unit,
    val onShowQr: () -> Unit,
    val onShare: () -> Unit,
    val onDismiss: () -> Unit,
)

/** A single token's icon (with network badge) and symbol, rendered in the token row. */
internal data class TokenIconUM(
    val symbol: String,
    val iconState: CurrencyIconState,
)