package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.tangem.domain.models.wallet.UserWallet

/**
 * `walletType` of `GET /api/referral/v1/promotion/` (a `@Query`, serialized by Retrofit via [toString]) and of the
 * registration body (serialized by Moshi via [Json]).
 */
enum class PromotionWalletType(private val value: String) {
    @Json(name = "card")
    CARD("card"),

    @Json(name = "mobile")
    MOBILE("mobile"),
    ;

    override fun toString(): String = value

    companion object {

        fun from(userWallet: UserWallet): PromotionWalletType = when (userWallet) {
            is UserWallet.Cold -> CARD
            is UserWallet.Hot -> MOBILE
        }
    }
}