package com.tangem.features.tangempay.multichain.choosenetwork

import kotlinx.collections.immutable.ImmutableList

internal data class PaymentChooseNetworkUM(
    val fastWay: ImmutableList<PaymentNetworkItemUM>,
    val otherWays: ImmutableList<PaymentNetworkItemUM>,
    val dismiss: () -> Unit,
)

internal data class PaymentNetworkItemUM(
    val id: String,
    val name: String,
    val tokensLabel: String,
    val iconResId: Int,
    val state: State,
    val onClick: () -> Unit,
    val onRetry: (() -> Unit)? = null,
) {

    enum class State { Idle, Loading, Error }
}