package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val ACCOUNT_SPECIFICATION_NAME = "SP_000006"

@Serializable
data class OrderRequest(
    @SerialName("data") val data: Data,
    @SerialName("idempotency_key") val idempotencyKey: String,
) {

    @Serializable
    sealed interface Data

    @Serializable
    @SerialName("CARD_ISSUE_VIRTUAL_RAIN")
    data class VirtualCardIssue(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("specification_name") val specificationName: String?,
        @SerialName("deeplink_value") val deeplinkValue: String? = null,
    ) : Data

    @Serializable
    @SerialName("CARD_ISSUE_VIRTUAL_RAIN_KYC")
    data class VirtualCardIssueKyc(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("specification_name") val specificationName: String?,
        @SerialName("deeplink_value") val deeplinkValue: String? = null,
    ) : Data

    @Serializable
    @SerialName("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2")
    data class VirtualCardIssueKycV2(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("specification_name") val specificationName: String?,
        @SerialName("deeplink_value") val deeplinkValue: String? = null,
    ) : Data

    @Serializable
    @SerialName("CARD_ISSUE_ADDITIONAL")
    data class AdditionalCardIssue(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("specification_name") val specificationName: String?,
        @SerialName("deeplink_value") val deeplinkValue: String? = null,
    ) : Data

    @Serializable
    @SerialName("CARD_ISSUE_PLASTIC_RAIN")
    data class PlasticCardIssue(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("specification_name") val specificationName: String,
        @SerialName("emboss_name") val embossName: String,
        @SerialName("shipping_address") val shippingAddress: ShippingAddress,
    ) : Data

    @Serializable
    @SerialName("CARD_REISSUE_PLASTIC_RAIN")
    data class PlasticCardReissue(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("source_product_instance_id") val sourceProductInstanceId: String,
        @SerialName("emboss_name") val embossName: String,
        @SerialName("shipping_address") val shippingAddress: ShippingAddress,
    ) : Data

    @Serializable
    @SerialName("CARD_ACTIVATION_PLASTIC_RAIN")
    data class PlasticCardActivation(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("product_instance_id") val productInstanceId: String,
        @SerialName("last_four_digits") val lastFourDigits: String,
    ) : Data

    @Serializable
    @SerialName("TARIFF_PLAN_TRANSITION")
    data class TariffPlanTransition(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("target_tariff_plan_id") val targetTariffPlanId: String,
        @SerialName("tariff_plan_transition_type") val tariffPlanTransitionType: String,
    ) : Data

    @Serializable
    @SerialName("SMART_CONTRACT_ISSUE_RAIN")
    data class SmartContractIssue(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("chain_id") val chainId: Int,
    ) : Data

    @Serializable
    @SerialName("ACCOUNT_ISSUE_VIRTUAL_RAIN")
    data class VirtualAccountIssue(
        @SerialName("deposit_address") val depositAddress: String,
        @SerialName("specification_name") val specificationName: String = ACCOUNT_SPECIFICATION_NAME,
    ) : Data

    @Serializable
    @SerialName("ACCOUNT_ISSUE_BANKING_RAIN")
    data class BankingAccountIssue(
        @SerialName("deposit_address") val depositAddress: String,
        @SerialName("signature") val signature: String,
        @SerialName("specification_name") val specificationName: String = ACCOUNT_SPECIFICATION_NAME,
    ) : Data

    @Serializable
    data class ShippingAddress(
        @SerialName("first_name") val firstName: String,
        @SerialName("last_name") val lastName: String,
        @SerialName("line1") val line1: String,
        @SerialName("line2") val line2: String?,
        @SerialName("city") val city: String,
        @SerialName("region") val region: String,
        @SerialName("postal_code") val postalCode: String,
        @SerialName("phone_number") val phoneNumber: String,
    )
}