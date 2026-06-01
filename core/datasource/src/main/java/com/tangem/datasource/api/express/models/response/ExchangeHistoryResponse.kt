package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeHistoryResponse(
    @Json(name = "data")
    val data: List<ExchangeRecord>,
    @Json(name = "next_cursor")
    val nextCursor: String,
    @Json(name = "has_more")
    val hasMore: Boolean,
) {

    @JsonClass(generateAdapter = true)
    data class ExchangeRecord(
        @Json(name = "tx_id")
        val txId: String,
        @Json(name = "status")
        val status: String,
        @Json(name = "provider")
        val provider: Provider,
        @Json(name = "from")
        val from: AssetRef,
        @Json(name = "to")
        val to: AssetRef,
        @Json(name = "payin_hash")
        val payinHash: String?,
        @Json(name = "payout_hash")
        val payoutHash: String?,
        @Json(name = "external_tx_id")
        val externalTxId: String?,
        @Json(name = "external_tx_url")
        val externalTxUrl: String?,
        @Json(name = "refund")
        val refund: RefundInfo?,
        @Json(name = "rate_type")
        val rateType: String,
        @Json(name = "created_at")
        val createdAt: Long,
        @Json(name = "updated_at")
        val updatedAt: Long,
    )

    @JsonClass(generateAdapter = true)
    data class Provider(
        @Json(name = "id")
        val id: String,
        @Json(name = "name")
        val name: String,
        @Json(name = "icon_url")
        val iconUrl: String,
        @Json(name = "provider_url")
        val providerUrl: String,
    )

    @JsonClass(generateAdapter = true)
    data class AssetRef(
        @Json(name = "network")
        val network: String,
        @Json(name = "token_id")
        val tokenId: String?,
        @Json(name = "raw_amount")
        val rawAmount: String,
        @Json(name = "decimals")
        val decimals: Int,
        @Json(name = "is_actual")
        val isActual: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    data class RefundInfo(
        @Json(name = "network")
        val network: String,
        @Json(name = "token_id")
        val tokenId: String?,
        @Json(name = "raw_amount")
        val rawAmount: String,
        @Json(name = "decimals")
        val decimals: Int,
        @Json(name = "hash")
        val hash: String?,
    )
}