package com.tangem.spend.datasource.visa.entity

import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MoneyAccountStatusValueDM {

    @Serializable
    @SerialName("empty")
    data object Empty : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("not_created")
    data object NotCreated : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("country_not_supported")
    data object CountryNotSupported : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("issuing")
    data class Issuing(
        @SerialName("step") val step: MoneyAccountStatusValue.IssuingStep,
        @SerialName("customer_id") val customerId: String?,
        @SerialName("balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("kyc_status")
    data class UnderReview(
        @SerialName("kyc_status") val kycStatus: KycStatus,
        @SerialName("customer_id") val customerId: String,
        @SerialName("balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("verification_declined")
    data class VerificationDeclined(
        @SerialName("customer_id") val customerId: String,
        @SerialName("balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("activating")
    data class Activating(
        @SerialName("customer_id") val customerId: String?,
        @SerialName("balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("active_account")
    data class ActiveAccount(
        @SerialName("customer_id") val customerId: String,
        @SerialName("product_instance_id") val productInstanceId: String,
        @SerialName("balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @Serializable
    @SerialName("deactivated")
    data class Deactivated(
        @SerialName("customer_id") val customerId: String,
        @SerialName("product_instance_id") val productInstanceId: String,
        @SerialName("balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @Serializable
    data class BalanceDM(
        @SerialName("crypto_amount") val cryptoAmount: SerializedBigDecimal,
        @SerialName("deposit_address") val depositAddress: String,
        @SerialName("currency_code") val currencyCode: String,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal?,
    )
}