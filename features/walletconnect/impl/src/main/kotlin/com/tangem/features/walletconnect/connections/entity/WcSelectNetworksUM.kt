package com.tangem.features.walletconnect.connections.entity

import kotlinx.collections.immutable.ImmutableList

internal data class WcSelectNetworksUM(
    val missing: ImmutableList<WcNetworkInfoItem.Required>,
    val required: ImmutableList<WcNetworkInfoItem.Checked>,
    val available: ImmutableList<WcNetworkInfoItem.Checkable>,
    val notAdded: ImmutableList<WcNetworkInfoItem.ReadOnly>,
    val onDone: () -> Unit,
    val doneButtonEnabled: Boolean,
)