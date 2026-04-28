@file:Suppress("BooleanPropertyNaming")
package com.tangem.datasource.local.visa.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import dev.onenowy.moshipolymorphicadapter.PolymorphicAdapterType
import dev.onenowy.moshipolymorphicadapter.annotations.NameLabel
import java.math.BigDecimal

/**
 * Payment account status for storage in the local cache.
 *
 * @see [com.tangem.domain.models.account.AccountStatus.Payment]
 */
@JsonClass(generateAdapter = true, generator = PolymorphicAdapterType.NAME_POLYMORPHIC_ADAPTER)
sealed interface PaymentAccountStatusValueDM {

    @NameLabel("empty")
    data class Empty(
        @Json(name = "empty") val marker: Boolean = true,
    ) : PaymentAccountStatusValueDM

    @NameLabel("not_created")
    data class NotCreated(
        @Json(name = "not_created") val marker: Boolean = true,
    ) : PaymentAccountStatusValueDM

    @NameLabel("kyc_status")
    data class UnderReview(
        @Json(name = "kyc_status") val kycStatus: KycStatus,
        @Json(name = "customer_id") val customerId: String,
    ) : PaymentAccountStatusValueDM

    @NameLabel("issuing_card")
    data class IssuingCard(
        @Json(name = "issuing_card") val marker: Boolean = true,
    ) : PaymentAccountStatusValueDM

    @NameLabel("active_account")
    data class ActiveAccount(
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "currency_code") val currencyCode: String,
        @Json(name = "deposit_address") val depositAddress: String?,
        @Json(name = "fiat_balance") val fiatBalance: FiatBalanceDM,
        @Json(name = "crypto_balance") val cryptoBalance: CryptoBalanceDM,
        @Json(name = "cards") val cards: List<TangemPayCard>,
    ) : PaymentAccountStatusValueDM

    @NameLabel("card_issue_failed")
    data class CardIssueFailed(
        @Json(name = "card_issue_failed") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String,
    ) : PaymentAccountStatusValueDM

    @JsonClass(generateAdapter = true)
    data class FiatBalanceDM(
        @Json(name = "available_balance") val availableBalance: BigDecimal,
        @Json(name = "currency") val currency: String,
    )

    @JsonClass(generateAdapter = true)
    data class CryptoBalanceDM(
        @Json(name = "id") val id: String,
        @Json(name = "chain_id") val chainId: Long,
        @Json(name = "deposit_address") val depositAddress: String,
        @Json(name = "token_contract_address") val tokenContractAddress: String,
        @Json(name = "balance") val balance: BigDecimal,
    )

    @JsonClass(generateAdapter = true)
    data class TangemPayCard(
        @Json(name = "id") val id: String,
        @Json(name = "has_pin_code") val hasPinCode: Boolean,
        @Json(name = "display_name") val displayName: String?,
        @Json(name = "actual_daily_limit") val actualDailyLimit: SerializedBigDecimal?,
        @Json(name = "admin_daily_limit") val adminDailyLimit: SerializedBigDecimal?,
        @Json(name = "is_frozen") val isFrozen: Boolean,
        @Json(name = "last_digits") val lastDigits: String,
    )
}