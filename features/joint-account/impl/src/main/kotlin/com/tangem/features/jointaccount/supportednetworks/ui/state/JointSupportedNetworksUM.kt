package com.tangem.features.jointaccount.supportednetworks.ui.state

import com.tangem.core.ui.ds.image.TangemIconUM
import kotlinx.collections.immutable.ImmutableList

internal data class JointSupportedNetworksUM(
    val networks: ImmutableList<NetworkItemUM>,
    val onDismiss: () -> Unit,
    val onGotItClick: () -> Unit,
) {

    data class NetworkItemUM(
        val id: String,
        val name: String,
        val symbol: String,
        val icon: TangemIconUM,
    )
}