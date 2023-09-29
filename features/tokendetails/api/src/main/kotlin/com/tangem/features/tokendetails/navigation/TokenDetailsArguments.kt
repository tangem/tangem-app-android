package com.tangem.features.tokendetails.navigation

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.parcelize.Parcelize

@Parcelize
data class TokenDetailsArguments(
    val currencyId: CryptoCurrency.ID,
    val currencyName: String,
    val currencySymbol: String,
    val iconUrl: String?,
    val coinType: CoinType,
) : Parcelable {

    @Parcelize
    sealed class CoinType : Parcelable {
        object Native : CoinType()

        /**
         * @param isCustom - Indicates whether the currency is a custom user-added currency or not.
         * @param standardName - token standard. Samples: ERC20, BEP20, BEP2, TRC20 and etc.
         * @param networkName - token's blockchain name. Ethereum, Tron and etc.
         */
        data class Token(
            val isCustom: Boolean,
            val standardName: String,
            val networkName: String,
            @DrawableRes val networkIcon: Int,
        ) : CoinType()
    }
}