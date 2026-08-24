package com.tangem.features.jointaccount.supportednetworks.ui.state

import androidx.annotation.DrawableRes
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
        @field:DrawableRes val iconResId: Int,
    )
}