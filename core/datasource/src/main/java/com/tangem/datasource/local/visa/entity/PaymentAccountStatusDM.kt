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
 * @see [com.tangem.domain.pay.PaymentAccountStatus]
 */
@JsonClass(generateAdapter = true, generator = PolymorphicAdapterType.NAME_POLYMORPHIC_ADAPTER)
sealed interface PaymentAccountStatusDM {

    @NameLabel("not_created")
    data class NotCreated(
        @Json(name = "not_created") val marker: Boolean = true,
    ) : PaymentAccountStatusDM

    @NameLabel("kyc_status")
    data class UnderReview(
        @Json(name = "kyc_status") val kycStatus: KycStatus,
    ) : PaymentAccountStatusDM

    @NameLabel("issuing_card")
    data class IssuingCard(
        @Json(name = "issuing_card") val marker: Boolean = true,
    ) : PaymentAccountStatusDM

    @NameLabel("locked")
    data class Locked(
        @Json(name = "locked") val marker: Boolean = true,
    ) : PaymentAccountStatusDM

    @NameLabel("balance")
    data class Loaded(
        @Json(name = "card_id") val cardId: String,
        @Json(name = "last_four_digits") val lastFourDigits: String,
        @Json(name = "balance") val balance: BigDecimal,
        @Json(name = "currency_code") val currencyCode: String,
        @Json(name = "deposit_address") val depositAddress: String?,
        @Json(name = "is_pin_set") val isPinSet: Boolean,
    ) : PaymentAccountStatusDM

    @NameLabel("card_issue_failed")
    data class CardIssueFailed(
        @Json(name = "card_issue_failed") val marker: Boolean = true,
    ) : PaymentAccountStatusDM
}