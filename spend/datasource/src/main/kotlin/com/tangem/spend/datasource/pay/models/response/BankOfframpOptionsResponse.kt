package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `bff-v2/v1/account/bank-offramp/options` — the tokens a withdrawal can start from and the bank
 * rails it can take, each with its fee and ETA.
 *
 * The source catalog is backend configuration rather than a closed list, so an unrecognised network or rail is
 * dropped by the converter instead of failing the whole response.
 */
@Serializable
data class BankOfframpOptionsResponse(
    @SerialName("result") val result: Result?,
) {

    @Serializable
    data class Result(
        @SerialName("sources") val sources: List<Source>?,
        @SerialName("destinations") val destinations: List<Destination>?,
    )

    @Serializable
    data class Source(
        @SerialName("name") val name: String?,
        @SerialName("is_testnet") val isTestnet: Boolean?,
        @SerialName("chain_id") val chainId: Long?,
        @SerialName("tokens") val tokens: List<Token>?,
    )

    @Serializable
    data class Token(
        @SerialName("token") val symbol: String?,
        @SerialName("token_contract_address") val contractAddress: String?,
    )

    @Serializable
    data class Destination(
        @SerialName("rail") val rail: String?,
        @SerialName("fee") val fee: Fee?,
        @SerialName("eta") val eta: Eta?,
    )

    @Serializable
    data class Fee(
        @SerialName("name") val name: String?,
        @SerialName("calculation_type") val calculationType: String?,
        @SerialName("amount") val amount: SerializedBigDecimal?,
        @SerialName("currency") val currency: String?,
        @SerialName("description") val description: String?,
    )

    @Serializable
    data class Eta(
        @SerialName("min_days") val minDays: Int?,
        @SerialName("max_days") val maxDays: Int?,
    )
}