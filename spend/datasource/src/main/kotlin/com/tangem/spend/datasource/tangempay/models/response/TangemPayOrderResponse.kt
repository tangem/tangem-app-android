package com.tangem.spend.datasource.tangempay.models.response

import com.tangem.domain.models.serialization.SerializedRawJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TangemPayOrderResponse(
    @SerialName("result") val result: Result,
) {

    @Serializable
    data class Result(
        @SerialName("id") val id: String,
        @SerialName("customer_id") val customerId: String,
        @SerialName("type") val type: String,
        @SerialName("status") val status: String,
        @SerialName("step") val step: String?,
        @SerialName("data") val data: OrderDataResponse,
        @SerialName("step_change_code") val stepChangeCode: Int,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
        @SerialName("error_code") val errorCode: String?,
    )
}

@Serializable
sealed interface OrderDataResponse {

    @Serializable
    @SerialName("CARD_FREEZE_RAIN")
    data class CardFreeze(
        @SerialName("product_instance_id") val productInstanceId: String,
    ) : OrderDataResponse

    @Serializable
    @SerialName("CARD_UNFREEZE_RAIN")
    data class CardUnfreeze(
        @SerialName("product_instance_id") val productInstanceId: String,
    ) : OrderDataResponse

    @Serializable
    @SerialName("CARD_CLOSE_RAIN")
    data class CardClose(
        @SerialName("product_instance_id") val productInstanceId: String,
    ) : OrderDataResponse

    @Serializable
    @SerialName("CARD_ACTIVATION_PLASTIC_RAIN")
    data class CardActivationPlastic(
        @SerialName("product_instance_id") val productInstanceId: String,
    ) : OrderDataResponse

    @Serializable
    @SerialName("CARD_REISSUE_VIRTUAL_RAIN")
    data class CardReissueVirtual(
        @SerialName("product_instance_id") val productInstanceId: String,
    ) : OrderDataResponse

    @Serializable
    @SerialName("CARD_REISSUE_PLASTIC_RAIN")
    data class CardReissuePlastic(
        @SerialName("source_product_instance_id") val sourceProductInstanceId: String,
        @SerialName("product_instance_id") val productInstanceId: String?,
        @SerialName("emboss_name") val embossName: String?,
    ) : OrderDataResponse

    sealed interface CardIssue : OrderDataResponse {
        val specificationName: String
        val customerWalletAddress: String
        val embossName: String?
        val productInstanceId: String?
        val paymentAccountId: String?
    }

    @Serializable
    @SerialName("CARD_ISSUE_VIRTUAL_RAIN")
    data class CardIssueVirtual(
        @SerialName("specification_name") override val specificationName: String,
        @SerialName("customer_wallet_address") override val customerWalletAddress: String,
        @SerialName("emboss_name") override val embossName: String?,
        @SerialName("product_instance_id") override val productInstanceId: String?,
        @SerialName("payment_account_id") override val paymentAccountId: String?,
    ) : CardIssue

    @Serializable
    @SerialName("CARD_ISSUE_VIRTUAL_RAIN_KYC")
    data class CardIssueVirtualKyc(
        @SerialName("specification_name") override val specificationName: String,
        @SerialName("customer_wallet_address") override val customerWalletAddress: String,
        @SerialName("emboss_name") override val embossName: String?,
        @SerialName("product_instance_id") override val productInstanceId: String?,
        @SerialName("payment_account_id") override val paymentAccountId: String?,
    ) : CardIssue

    @Serializable
    @SerialName("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2")
    data class CardIssueVirtualKycV2(
        @SerialName("specification_name") override val specificationName: String,
        @SerialName("customer_wallet_address") override val customerWalletAddress: String,
        @SerialName("emboss_name") override val embossName: String?,
        @SerialName("product_instance_id") override val productInstanceId: String?,
        @SerialName("payment_account_id") override val paymentAccountId: String?,
    ) : CardIssue

    @Serializable
    @SerialName("CARD_ISSUE_PLASTIC_RAIN")
    data class CardIssuePlastic(
        @SerialName("specification_name") override val specificationName: String,
        @SerialName("customer_wallet_address") override val customerWalletAddress: String,
        @SerialName("emboss_name") override val embossName: String?,
        @SerialName("product_instance_id") override val productInstanceId: String?,
        @SerialName("payment_account_id") override val paymentAccountId: String?,
    ) : CardIssue

    sealed interface AccountIssue : OrderDataResponse {
        val specificationName: String
        val depositAddress: String
        val productInstanceId: String?
        val paymentAccountId: String?
        val automationId: String?
    }

    @Serializable
    @SerialName("ACCOUNT_ISSUE_VIRTUAL_RAIN")
    data class AccountIssueVirtual(
        @SerialName("specification_name") override val specificationName: String,
        @SerialName("deposit_address") override val depositAddress: String,
        @SerialName("product_instance_id") override val productInstanceId: String?,
        @SerialName("payment_account_id") override val paymentAccountId: String?,
        @SerialName("automation_id") override val automationId: String?,
    ) : AccountIssue

    @Serializable
    @SerialName("ACCOUNT_ISSUE_BANKING_RAIN")
    data class AccountIssueBanking(
        @SerialName("specification_name") override val specificationName: String,
        @SerialName("deposit_address") override val depositAddress: String,
        @SerialName("product_instance_id") override val productInstanceId: String?,
        @SerialName("payment_account_id") override val paymentAccountId: String?,
        @SerialName("automation_id") override val automationId: String?,
    ) : AccountIssue

    @Serializable
    @SerialName("WITHDRAWAL_RAIN")
    data class Withdrawal(
        @SerialName("chain_id") val chainId: Int,
        @SerialName("transaction_hash") val transactionHash: String?,
    ) : OrderDataResponse

    @Serializable
    @SerialName("BANK_OFFRAMP_RAIN")
    data class BankOfframp(
        @SerialName("chain_id") val chainId: Int,
        @SerialName("transaction_hash") val transactionHash: String?,
        @SerialName("bank_account_id") val bankAccountId: String?,
        @SerialName("withdraw_data") val withdrawData: WithdrawData?,
    ) : OrderDataResponse {

        @Serializable
        data class WithdrawData(
            @SerialName("hash") val hash: String,
            @SerialName("salt") val salt: String,
            @SerialName("sender_address") val senderAddress: String,
            @SerialName("structured_data") val structuredData: SerializedRawJson,
        )
    }

    @Serializable
    @SerialName("SMART_CONTRACT_ISSUE_RAIN")
    data class SmartContractIssue(
        @SerialName("chain_id") val chainId: Int,
    ) : OrderDataResponse

    @Serializable
    @SerialName("TARIFF_PLAN_TRANSITION")
    data class TariffPlanTransition(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("target_tariff_plan_id") val targetTariffPlanId: String,
        @SerialName("tariff_plan_transition_type") val transitionType: String,
    ) : OrderDataResponse

    @Serializable
    @SerialName("ONBOARDING_RAIN")
    data class Onboarding(
        @SerialName("customer_wallet_address") val customerWalletAddress: String,
        @SerialName("tariff_plan_transition") val tariffPlanTransition: TariffPlanTransitionData?,
        @SerialName("account_issue_banking") val accountIssueBanking: AccountIssueBankingData?,
        @SerialName("tariff_plan_transition_order_id") val tariffPlanTransitionOrderId: String?,
        @SerialName("account_issue_banking_order_id") val accountIssueBankingOrderId: String?,
    ) : OrderDataResponse {

        @Serializable
        data class TariffPlanTransitionData(
            @SerialName("target_tariff_plan_id") val targetTariffPlanId: String,
            @SerialName("tariff_plan_transition_type") val transitionType: String,
        )

        @Serializable
        data class AccountIssueBankingData(
            @SerialName("deposit_address") val depositAddress: String,
        )
    }

    @Serializable
    data class Unknown(
        @SerialName("wire_type") val type: String?,
        @SerialName("payload") val payload: JsonObject?,
    ) : OrderDataResponse
}