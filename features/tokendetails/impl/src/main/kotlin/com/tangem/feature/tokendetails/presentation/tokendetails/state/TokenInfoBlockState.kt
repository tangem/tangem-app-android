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
            val network: String,
            val blockchain: String,
            @DrawableRes val networkIcon: Int,
        ) : Currency()
    }
}
