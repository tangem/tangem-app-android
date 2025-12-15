package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.wallet.UserWallet

@JsonClass(generateAdapter = false)
enum class WalletType {
    @Json(name = "card")
    COLD,

    @Json(name = "mobile")
    HOT,
    ;

    companion object {

        fun from(userWallet: UserWallet?): WalletType? {
            return when (userWallet) {
                is UserWallet.Cold -> COLD
                is UserWallet.Hot -> HOT
                null -> null
            }
        }
    }
}