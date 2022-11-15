package com.tangem.datasource.api.referral.models

import com.squareup.moshi.Json
import org.joda.time.DateTime

/**
 * Main response class for referral API
 * contains all necessary info about users program status
 */
data class ReferralResponse(
    @Json(name = "conditions") val conditions: Conditions,
    @Json(name = "referral") val referral: Referral?,
)

data class Conditions(
    @Json(name = "award") val award: Int,
    @Json(name = "discount") val discount: Int,
    @Json(name = "discountType") val discountType: DiscountType,
    @Json(name = "tosLink") val tosLink: String,
    @Json(name = "tokens") val tokens: List<Token>,
)

data class Token(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "networkId") val networkId: String?,
    @Json(name = "contractAddress") val contractAddress: String?,
    @Json(name = "decimalCount") val decimalCount: Int,
)

data class Referral(
    @Json(name = "shareLink") val shareLink: String,
    @Json(name = "address") val address: String,
    @Json(name = "promocode") val promocode: String,
    @Json(name = "walletsPurchased") val walletsPurchased: Int,
    @Json(name = "termsAcceptedAt") val termsAcceptedAt: DateTime?,
)

enum class DiscountType {
    PERCENTAGE, VALUE
}