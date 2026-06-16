@file:Suppress("BooleanPropertyNaming")
package com.tangem.datasource.local.visa.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.kyc.KycStatus
import dev.onenowy.moshipolymorphicadapter.PolymorphicAdapterType
import dev.onenowy.moshipolymorphicadapter.annotations.NameLabel
import java.math.BigDecimal

/**
 * Virtual account status for storage in the local cache.
 *
 * @see [com.tangem.domain.models.account.AccountStatus.Virtual]
 */
@JsonClass(generateAdapter = true, generator = PolymorphicAdapterType.NAME_POLYMORPHIC_ADAPTER)
sealed interface VirtualAccountStatusValueDM {

    @NameLabel("empty")
    data class Empty(
        @Json(name = "empty") val marker: Boolean = true,
    ) : VirtualAccountStatusValueDM

    @NameLabel("not_created")
    data class NotCreated(
        @Json(name = "not_created") val marker: Boolean = true,
    ) : VirtualAccountStatusValueDM

    @NameLabel("kyc_status")
    data class UnderReview(
        @Json(name = "kyc_status") val kycStatus: KycStatus,
        @Json(name = "customer_id") val customerId: String,
    ) : VirtualAccountStatusValueDM

    @NameLabel("provisioning")
    data class Provisioning(
        @Json(name = "provisioning") val marker: Boolean = true,
    ) : VirtualAccountStatusValueDM

    @NameLabel("country_not_supported")
    data class CountryNotSupported(
        @Json(name = "country_not_supported") val marker: Boolean = true,
    ) : VirtualAccountStatusValueDM

    @NameLabel("active_account")
    data class ActiveAccount(
        @Json(name = "active_account") val marker: Boolean = true,
        @Json(name = "customer_id") val customerId: String,
        @Json(name = "currency_code") val currencyCode: String,
        @Json(name = "deposit_address") val depositAddress: String?,
        @Json(name = "fiat_balance") val fiatBalance: FiatBalanceDM,
        @Json(name = "crypto_balance") val cryptoBalance: CryptoBalanceDM,
        @Json(name = "fiat_rate") val fiatRate: BigDecimal?,
        @Json(name = "available_for_withdrawal") val availableForWithdrawal: BigDecimal,
    ) : VirtualAccountStatusValueDM

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