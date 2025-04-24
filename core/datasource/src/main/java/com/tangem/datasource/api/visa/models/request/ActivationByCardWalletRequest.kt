package com.tangem.datasource.api.visa.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActivationByCardWalletRequest(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "card_wallet") val cardWallet: CardWallet,
    @Json(name = "deploy_acceptance_signature") val deployAcceptanceSignature: String,
    @Json(name = "otp") val otp: Otp,
) {
    @JsonClass(generateAdapter = true)
    data class CardWallet(
        @Json(name = "address") val address: String,
        @Json(name = "card_wallet_confirmation") val cardWalletConfirmation: CardWalletConfirmation?,
    )

    @JsonClass(generateAdapter = true)
    data class CardWalletConfirmation(
        @Json(name = "challenge") val challenge: String,
        @Json(name = "wallet_salt") val walletSalt: String,
        @Json(name = "wallet_signature") val walletSignature: String,
        @Json(name = "card_salt") val cardSalt: String,
        @Json(name = "card_signature") val cardSignature: String,
    )

    @JsonClass(generateAdapter = true)
    data class Otp(
        @Json(name = "root_otp") val rootOtp: String,
        @Json(name = "counter") val counter: Int,
    )
}