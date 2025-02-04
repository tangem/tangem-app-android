package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.LocalDate

@JsonClass(generateAdapter = true)
data class ReferralResponse(
    @Json(name = "conditions") val conditions: Conditions,
    @Json(name = "referral") val referral: Referral?,
    @Json(name = "expectedAwards") val expectedAwards: ExpectedAwards?,
) {

    @JsonClass(generateAdapter = true)
    data class Conditions(
        @Json(name = "discount") val discount: Discount,
        @Json(name = "awards") val awards: List<Award>,
        @Json(name = "tosLink") val tosLink: String,
    ) {

        @JsonClass(generateAdapter = true)
        data class Discount(
            @Json(name = "amount") val amount: Int,
            @Json(name = "type") val discountType: String,
        )

        @JsonClass(generateAdapter = true)
        data class Award(
            @Json(name = "amount") val amount: Int,
            @Json(name = "token") val token: Token,
        ) {

            @JsonClass(generateAdapter = true)
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

    @JsonClass(generateAdapter = true)
    data class Referral(
        @Json(name = "shareLink") val shareLink: String,
        @Json(name = "address") val address: String,
        @Json(name = "promoCode") val promocode: String,
        @Json(name = "walletsPurchased") val walletsPurchased: Int,
        @Json(name = "termsAcceptedAt") val termsAcceptedAt: String?,
    )

    @JsonClass(generateAdapter = true)
    data class ExpectedAwards(
        @Json(name = "numberOfWallets") val numberOfWallets: Int,
        @Json(name = "list") val list: List<AwardItem>,
    ) {

        @JsonClass(generateAdapter = true)
        data class AwardItem(
            @Json(name = "currency") val currency: String,
            @Json(name = "paymentDate") val paymentDate: LocalDate,
            @Json(name = "amount") val amount: Int,
        )
    }
}