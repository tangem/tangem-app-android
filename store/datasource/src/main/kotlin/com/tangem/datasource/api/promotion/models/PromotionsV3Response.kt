package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/** Campaigns eligible for a single wallet. Eligibility is resolved by the backend, so it is not modelled here. */
@JsonClass(generateAdapter = true)
data class PromotionsV3Response(
    @Json(name = "promotions") val promotions: List<PromotionDto>,
) {

    @JsonClass(generateAdapter = true)
    data class PromotionDto(
        @Json(name = "name") val name: String,
        @Json(name = "status") val status: String,
        @Json(name = "startAt") val startAt: String,
        @Json(name = "endAt") val endAt: String,
        @Json(name = "alreadyEnrolled") val isAlreadyEnrolled: Boolean,
        @Json(name = "properties") val properties: Properties,
        @Json(name = "content") val content: Content,
    )

    @JsonClass(generateAdapter = true)
    data class Properties(
        @Json(name = "payoutNeeded") val isPayoutNeeded: Boolean,
        @Json(name = "payoutTokens") val payoutTokens: List<PayoutToken>?,
        @Json(name = "matchMode") val matchMode: MatchMode,
    )

    /** [tokenId], [tokenName], [tokenSymbol] and [decimals] are resolved by the backend from `/v1/coins`. */
    @JsonClass(generateAdapter = true)
    data class PayoutToken(
        @Json(name = "tokenAddress") val tokenAddress: String,
        @Json(name = "networkId") val networkId: String,
        @Json(name = "tokenId") val tokenId: String?,
        @Json(name = "tokenName") val tokenName: String?,
        @Json(name = "tokenSymbol") val tokenSymbol: String?,
        @Json(name = "decimals") val decimals: Int?,
    )

    @JsonClass(generateAdapter = true)
    data class Content(
        @Json(name = "campaignNameKey") val campaignNameKey: String,
        @Json(name = "publicDescription") val publicDescription: List<DescriptionBlock>,
        @Json(name = "publicImage") val publicImage: String,
        @Json(name = "learnMoreLink") val learnMoreLink: Link?,
        @Json(name = "tncLink") val tncLink: Link,
        @Json(name = "actionButtonNameKey") val actionButtonNameKey: String,
    )

    /** One flat shape for every block [type], so a type unknown to this app version parses as [BlockType.UNKNOWN]. */
    @JsonClass(generateAdapter = true)
    data class DescriptionBlock(
        @Json(name = "type") val type: BlockType,
        @Json(name = "titleKey") val titleKey: String?,
        @Json(name = "textKey") val textKey: String?,
        @Json(name = "items") val items: List<BlockItem>?,
        @Json(name = "headersKeys") val headersKeys: List<String>?,
        @Json(name = "columnTypes") val columnTypes: List<ValueType>?,
        @Json(name = "rows") val rows: List<List<TableCell>>?,
    )

    /** [value] is a number or a string depending on [valueType]; Moshi reads both as a string. */
    @JsonClass(generateAdapter = true)
    data class BlockItem(
        @Json(name = "labelKey") val labelKey: String,
        @Json(name = "value") val value: String,
        @Json(name = "valueType") val valueType: ValueType,
        @Json(name = "currency") val currency: String?,
    )

    @JsonClass(generateAdapter = true)
    data class TableCell(
        @Json(name = "textKey") val textKey: String?,
        @Json(name = "value") val value: BigDecimal?,
        @Json(name = "currency") val currency: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Link(
        @Json(name = "textKey") val textKey: String,
        @Json(name = "url") val url: String,
    )

    /** Values unknown to this app version parse as `UNKNOWN`, see `addPromotionEnumFallbackAdapters`. */
    @JsonClass(generateAdapter = false)
    enum class MatchMode {
        @Json(name = "strict")
        STRICT,

        @Json(name = "linked_wallet")
        LINKED_WALLET,

        UNKNOWN,
    }

    @JsonClass(generateAdapter = false)
    enum class BlockType {
        @Json(name = "text")
        TEXT,

        @Json(name = "block")
        BLOCK,

        @Json(name = "table")
        TABLE,

        @Json(name = "highlight")
        HIGHLIGHT,

        UNKNOWN,
    }

    @JsonClass(generateAdapter = false)
    enum class ValueType {
        @Json(name = "text")
        TEXT,

        @Json(name = "number")
        NUMBER,

        @Json(name = "percent")
        PERCENT,

        @Json(name = "money")
        MONEY,

        UNKNOWN,
    }
}