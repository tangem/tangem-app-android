package com.tangem.datasource.api.onramp.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampHistoryResponse(
    @Json(name = "data")
    val data: List<OnrampRecord>,
    @Json(name = "next_cursor")
    val nextCursor: String,
    @Json(name = "has_more")
    val hasMore: Boolean,
) {

    @JsonClass(generateAdapter = true)
    data class OnrampRecord(
        @Json(name = "tx_id")
        val txId: String,
        @Json(name = "status")
        val status: String,
        @Json(name = "provider")
        val provider: Provider,
        @Json(name = "from")
        val from: FiatRef,
        @Json(name = "to")
        val to: OnrampAssetRef,
        @Json(name = "payout_hash")
        val payoutHash: String?,
        @Json(name = "external_tx_id")
        val externalTxId: String?,
        @Json(name = "external_tx_url")
        val externalTxUrl: String?,
        @Json(name = "refund")
        val refund: OnrampRefundInfo?,
        @Json(name = "rate_type")
        val rateType: String,
        @Json(name = "fail_reason")
        val failReason: String?,
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
    data class FiatRef(
        @Json(name = "currency_code")
        val currencyCode: String,
        @Json(name = "amount")
        val amount: String,
    )

    @JsonClass(generateAdapter = true)
    data class OnrampAssetRef(
        @Json(name = "network")
        val network: String,
        @Json(name = "token_id")
        val tokenId: String?,
        @Json(name = "expected_raw_amount")
        val expectedRawAmount: String,
        @Json(name = "actual_raw_amount")
        val actualRawAmount: String?,
        @Json(name = "decimals")
        val decimals: Int,
    )

    @JsonClass(generateAdapter = true)
    data class OnrampRefundInfo(
        @Json(name = "currency_code")
        val currencyCode: String,
        @Json(name = "amount")
        val amount: String,
    )
}