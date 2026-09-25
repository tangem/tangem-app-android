package com.tangem.datasource.api.promotion.models

import com.tangem.domain.models.wallet.UserWallet

/** `walletType` query value of `GET /v3/promotion`; Retrofit serializes a `@Query` enum via [toString]. */
enum class PromotionWalletType(private val value: String) {
    CARD("card"),
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