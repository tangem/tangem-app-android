package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Response of `bff-v2/v1/account/bank-offramp/options` — the tokens a withdrawal can start from and the bank
 * rails it can take, each with its fee and ETA.
 *
 * The source catalog is backend configuration rather than a closed list, so an unrecognised network or rail is
 * dropped by the converter instead of failing the whole response.
 */
@JsonClass(generateAdapter = true)
data class BankOfframpOptionsResponse(
    @Json(name = "result") val result: Result?,
) {

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "sources") val sources: List<Source>?,
        @Json(name = "destinations") val destinations: List<Destination>?,
    )

    @JsonClass(generateAdapter = true)
    data class Source(
        @Json(name = "name") val name: String?,
        @Json(name = "is_testnet") val isTestnet: Boolean?,
        @Json(name = "chain_id") val chainId: Long?,
        @Json(name = "tokens") val tokens: List<Token>?,
    )

    @JsonClass(generateAdapter = true)
    data class Token(
        @Json(name = "token") val symbol: String?,
        @Json(name = "token_contract_address") val contractAddress: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Destination(
        @Json(name = "rail") val rail: String?,
        @Json(name = "fee") val fee: Fee?,
        @Json(name = "eta") val eta: Eta?,
    )

    @JsonClass(generateAdapter = true)
    data class Fee(
        @Json(name = "name") val name: String?,
        @Json(name = "calculation_type") val calculationType: String?,
        @Json(name = "amount") val amount: BigDecimal?,
        @Json(name = "currency") val currency: String?,
        @Json(name = "description") val description: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Eta(
        @Json(name = "min_days") val minDays: Int?,
        @Json(name = "max_days") val maxDays: Int?,
    )
}