package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.annotation.DrawableRes

data class TokenInfoBlockState(
    val name: String,
    val iconUrl: String,
    val currency: Currency,
) {
    sealed class Currency {
        object Native : Currency()
        data class Token(
            val networkName: String,
            val blockchainName: String,
            @DrawableRes val networkIcon: Int,
        ) : Currency()
    }
}
