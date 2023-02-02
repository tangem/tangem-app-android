package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

/**
 * Main response class for referral API
 * contains all necessary info about users program status
 */
data class ReferralResponse(
    @Json(name = "conditions") val conditions: Conditions,
    @Json(name = "referral") val referral: Referral?,
) {

    data class Conditions(
        @Json(name = "discount") val discount: Discount,
        @Json(name = "awards") val awards: List<Award>,
        @Json(name = "tosLink") val tosLink: String,
    ) {

        data class Discount(
            @Json(name = "amount") val amount: Int,
            @Json(name = "type") val discountType: String,
        )

        data class Award(
            @Json(name = "amount") val amount: Int,
            @Json(name = "token") val token: Token,
        ) {

            data class Token(
                @Json(name = "id") val id: String,
                @Json(name = "name") val name: String,
                @Json(name = "symbol") val symbol: String,
                @Json(name = "networkId") val networkId: String,
                @Json(name = "contractAddress") val contractAddress: String?,
                @Json(name = "decimalCount") val decimalCount: Int?,
            )
        }
    }

    data class Referral(
        @Json(name = "shareLink") val shareLink: String,
        @Json(name = "address") val address: String,
        @Json(name = "promoCode") val promocode: String,
        @Json(name = "walletsPurchased") val walletsPurchased: Int,
        @Json(name = "termsAcceptedAt") val termsAcceptedAt: String?,
    )
}
