@file:Suppress("BooleanPropertyNaming")
package com.tangem.datasource.local.visa.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.kyc.KycStatus
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

    @NameLabel("active_card")
    data class ActiveCard(
        @Json(name = "active_card") val isLocked: Boolean,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "card_id") val cardId: String,
        @Json(name = "last_four_digits") val lastFourDigits: String,
        @Json(name = "currency_code") val currencyCode: String,
        @Json(name = "deposit_address") val depositAddress: String?,
        @Json(name = "is_pin_set") val isPinSet: Boolean,
        @Json(name = "fiat_balance") val fiatBalance: FiatBalanceDM,
        @Json(name = "crypto_balance") val cryptoBalance: CryptoBalanceDM,
    ) : PaymentAccountStatusValueDM

    @NameLabel("card_issue_failed")
    data class CardIssueFailed(
        @Json(name = "card_issue_failed") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String,
    ) : PaymentAccountStatusValueDM

    @NameLabel("deactivated_account")
    data class DeactivatedAccount(
        @Json(name = "deactivated_account") val marker: Boolean = true,
        @Json(name = "fiat_balance") val fiatBalance: FiatBalanceDM,
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
}