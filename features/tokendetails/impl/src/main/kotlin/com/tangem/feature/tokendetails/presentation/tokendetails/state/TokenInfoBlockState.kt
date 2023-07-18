package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.annotation.DrawableRes

data class TokenInfoBlockState(
    val name: String,
    val iconUrl: String,
    val currency: Currency,
) {
    sealed class Currency {
        object Native : Currency()

        /**
         * @param networkName - token standard. Samples: ERC20, BEP20, BEP2, TRC20 and etc.
         * @param blockchainName - token's blockchain name. Ethereum, Tron and etc.
         */
        data class Token(
            val networkName: String,
            val blockchainName: String,
            @DrawableRes val networkIcon: Int,
        ) : Currency()
    }
}