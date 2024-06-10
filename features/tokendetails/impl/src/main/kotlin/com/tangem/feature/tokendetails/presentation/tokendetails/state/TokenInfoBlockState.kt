package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

internal data class TokenInfoBlockState(
    val name: String,
    val iconState: IconState,
    val currency: Currency,
) {
    @Immutable
    sealed class Currency {
        data object Native : Currency()

        /**
         * @param standardName - token standard. Samples: ERC20, BEP20, BEP2, TRC20 and etc.
         * @param networkName - token's network name.
         * @param networkIcon - token's network icon.
         */
        data class Token(
            val standardName: String?,
            val networkName: String,
            @DrawableRes val networkIcon: Int,
        ) : Currency()
    }
}