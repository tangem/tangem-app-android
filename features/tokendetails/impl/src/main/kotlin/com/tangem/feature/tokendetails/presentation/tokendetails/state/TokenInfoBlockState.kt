package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.annotation.DrawableRes

data class TokenInfoBlockState(
    val name: String,
    val iconUrl: String,
    val network: Network,
) {
    sealed class Network {
        object MainNetwork : Network()
        data class TokenNetwork(
            val network: String,
            val blockchain: String,
            @DrawableRes val networkIcon: Int,
        ) : Network()
    }
}
