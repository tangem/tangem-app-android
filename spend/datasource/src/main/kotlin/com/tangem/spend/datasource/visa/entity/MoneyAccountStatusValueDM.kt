@file:Suppress("BooleanPropertyNaming")
package com.tangem.spend.datasource.visa.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import dev.onenowy.moshipolymorphicadapter.PolymorphicAdapterType
import dev.onenowy.moshipolymorphicadapter.annotations.NameLabel
import java.math.BigDecimal

@JsonClass(generateAdapter = true, generator = PolymorphicAdapterType.NAME_POLYMORPHIC_ADAPTER)
sealed interface MoneyAccountStatusValueDM {

    @NameLabel("empty")
    @JsonClass(generateAdapter = true)
    data class Empty(
        @Json(name = "empty") val marker: Boolean = true,
    ) : MoneyAccountStatusValueDM

    @NameLabel("not_created")
    @JsonClass(generateAdapter = true)
    data class NotCreated(
        @Json(name = "not_created") val marker: Boolean = true,
    ) : MoneyAccountStatusValueDM

    @NameLabel("country_not_supported")
    @JsonClass(generateAdapter = true)
    data class CountryNotSupported(
        @Json(name = "country_not_supported") val marker: Boolean = true,
    ) : MoneyAccountStatusValueDM

    @NameLabel("issuing")
    @JsonClass(generateAdapter = true)
    data class Issuing(
        @Json(name = "issuing") val marker: Boolean = true,
        @Json(name = "step") val step: MoneyAccountStatusValue.IssuingStep,
        @Json(name = "customer_id") val customerId: String?,
        @Json(name = "balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @NameLabel("kyc_status")
    @JsonClass(generateAdapter = true)
    data class UnderReview(
        @Json(name = "kyc_status") val kycStatus: KycStatus,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @NameLabel("verification_declined")
    @JsonClass(generateAdapter = true)
    data class VerificationDeclined(
        @Json(name = "verification_declined") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @NameLabel("activating")
    @JsonClass(generateAdapter = true)
    data class Activating(
        @Json(name = "activating") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String?,
        @Json(name = "balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @NameLabel("active_account")
    @JsonClass(generateAdapter = true)
    data class ActiveAccount(
        @Json(name = "active_account") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "product_instance_id") val productInstanceId: String,
        @Json(name = "balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @NameLabel("deactivated")
    @JsonClass(generateAdapter = true)
    data class Deactivated(
        @Json(name = "deactivated") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "product_instance_id") val productInstanceId: String,
        @Json(name = "balance") val balance: BalanceDM?,
    ) : MoneyAccountStatusValueDM

    @JsonClass(generateAdapter = true)
    data class BalanceDM(
        @Json(name = "crypto_amount") val cryptoAmount: BigDecimal,
        @Json(name = "deposit_address") val depositAddress: String,
        @Json(name = "currency_code") val currencyCode: String,
        @Json(name = "fiat_rate") val fiatRate: BigDecimal?,
    )
}