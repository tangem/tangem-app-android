package com.tangem.spend.datasource.visa.entity

import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payment account status for storage in the local cache.
 *
 * @see [com.tangem.domain.models.account.AccountStatus.Payment]
 */
@Serializable
sealed interface PaymentAccountStatusValueDM {

    @Serializable
    @SerialName("empty")
    data object Empty : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("not_created")
    data object NotCreated : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("kyc_required")
    data class KycRequired(
        @SerialName("customer_id") val customerId: String?,
    ) : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("kyc_status")
    data class UnderReview(
        @SerialName("kyc_status") val kycStatus: KycStatus,
        @SerialName("customer_id") val customerId: String,
    ) : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("issuing_card")
    data object IssuingCard : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("active_account")
    data class ActiveAccount(
        @SerialName("customer_id") val customerId: String,
        @SerialName("currency_code") val currencyCode: String?,
        @SerialName("payment_account_address") val paymentAccountAddress: String?,
        @SerialName("fiat_balance") val fiatBalance: FiatBalanceDM?,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal?,
        @SerialName("cards") val cards: List<TangemPayCard>,
    ) : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("card_issue_failed")
    data class CardIssueFailed(
        @SerialName("customer_id") val customerId: String,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal? = null,
        @SerialName("balance") val balance: BalanceDM? = null,
    ) : PaymentAccountStatusValueDM

    @Serializable
    @SerialName("deactivated_account")
    data class DeactivatedAccount(
        @SerialName("customer_id") val customerId: String,
        @SerialName("fiat_rate") val fiatRate: SerializedBigDecimal?,
        @SerialName("fiat_balance") val fiatBalance: FiatBalanceDM?,
    ) : PaymentAccountStatusValueDM

    /** @see [com.tangem.domain.models.account.PaymentAccountStatusValue.Balance] */
    @Serializable
    data class BalanceDM(
        @SerialName("fiat_balance") val fiatBalance: FiatBalanceDM,
    )

    @Serializable
    data class FiatBalanceDM(
        @SerialName("available_balance") val availableBalance: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
    )

    @Serializable
    data class TangemPayCard(
        @SerialName("id") val id: String,
        @SerialName("product_instance_id") val productInstanceId: String,
        @SerialName("card_status") val cardStatus: String,
        @SerialName("has_pin_code") val hasPinCode: Boolean,
        @SerialName("display_name") val displayName: String?,
        @SerialName("actual_daily_limit") val actualDailyLimit: SerializedBigDecimal?,
        @SerialName("admin_daily_limit") val adminDailyLimit: SerializedBigDecimal?,
        @SerialName("frozen_state") val frozenState: String,
        @SerialName("last_digits") val lastDigits: String,
        @SerialName("images") val images: List<ImageDM>,
        @SerialName("state") val state: String,
        @SerialName("emboss_name") val embossName: String?,
        @SerialName("card_type") val cardType: String? = null,
    )

    @Serializable
    data class ImageDM(
        @SerialName("type") val type: String,
        @SerialName("url") val url: String,
    )
}