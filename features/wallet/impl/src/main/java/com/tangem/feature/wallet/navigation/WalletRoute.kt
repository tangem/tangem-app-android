package com.tangem.feature.wallet.navigation

import kotlinx.serialization.Serializable

@Serializable
internal sealed class WalletRoute {

    @Serializable
    data object Wallet : WalletRoute()
}