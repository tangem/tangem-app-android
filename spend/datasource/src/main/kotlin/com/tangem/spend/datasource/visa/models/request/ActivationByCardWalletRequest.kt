package com.tangem.spend.datasource.visa.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivationByCardWalletRequest(
    @SerialName("order_id") val orderId: String,
    @SerialName("card_wallet") val cardWallet: CardWallet,
    @SerialName("deploy_acceptance_signature") val deployAcceptanceSignature: String,
    @SerialName("otp") val otp: Otp,
) {
    @Serializable
    data class CardWallet(
        @SerialName("address") val address: String,
        @SerialName("card_wallet_confirmation") val cardWalletConfirmation: CardWalletConfirmation?,
    )

    @Serializable
    data class CardWalletConfirmation(
        @SerialName("challenge") val challenge: String,
        @SerialName("wallet_salt") val walletSalt: String,
        @SerialName("wallet_signature") val walletSignature: String,
        @SerialName("card_salt") val cardSalt: String,
        @SerialName("card_signature") val cardSignature: String,
    )

    @Serializable
    data class Otp(
        @SerialName("root_otp") val rootOtp: String,
        @SerialName("counter") val counter: Int,
    )
}