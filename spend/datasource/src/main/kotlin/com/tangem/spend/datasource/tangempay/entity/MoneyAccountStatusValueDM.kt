package com.tangem.spend.datasource.tangempay.entity

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MoneyAccountStatusValueDM {

    @Serializable
    @SerialName("empty")
    data object Empty : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("activating")
    data class Activating(
        @SerialName("customer_id") val customerId: String,
        @SerialName("step") val step: StepDM,
    ) : MoneyAccountStatusValueDM {

        @Serializable
        enum class StepDM {
            @SerialName("issuing")
            ISSUING,

            @SerialName("awaiting_deposit")
            AWAITING_DEPOSIT,
        }
    }

    @Serializable
    @SerialName("failed")
    data class Failed(
        @SerialName("customer_id") val customerId: String,
    ) : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("active_account")
    data class ActiveAccount(
        @SerialName("customer_id") val customerId: String,
        @SerialName("product_instance_id") val productInstanceId: String? = null,
        @SerialName("balance") val balance: BalanceDM?,
        @SerialName("is_kyc_additional_info_required") val isKycAdditionalInfoRequired: Boolean = false,
    ) : MoneyAccountStatusValueDM

    @Serializable
    data class BalanceDM(
        @SerialName("crypto_amount") val cryptoAmount: SerializedBigDecimal,
        @SerialName("deposit_address") val depositAddress: String,
        @SerialName("currency_code") val currencyCode: String,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal?,
        @SerialName("yield_supply_status") val yieldSupplyStatus: YieldSupplyStatusDM? = null,
    )

    @Serializable
    data class YieldSupplyStatusDM(
        @SerialName("is_active") val isActive: Boolean,
        @SerialName("is_initialized") val isInitialized: Boolean,
        @SerialName("is_allowed_to_spend") val isAllowedToSpend: Boolean,
        @SerialName("effective_protocol_balance") val effectiveProtocolBalance: SerializedBigDecimal?,
    )
}