package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class PromotionsV3Response(
    @Json(name = "promotions") val promotions: List<PromotionDto>,
) {

    @JsonClass(generateAdapter = true)
    data class PromotionDto(
        @Json(name = "id") val id: Int?,
        @Json(name = "name") val name: String,
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
        @Json(name = "campaignName") val campaignName: String,
        @Json(name = "campaignDescription") val campaignDescription: String?,
        @Json(name = "publicDescription") val publicDescription: List<DescriptionBlock>,
        @Json(name = "publicImage") val publicImage: String,
        @Json(name = "tncLink") val tncLink: Link,
        @Json(name = "actionButtonName") val actionButtonName: String,
    )

    /** One flat shape for every block [type], so a type unknown to this app version parses as [BlockType.UNKNOWN]. */
    @JsonClass(generateAdapter = true)
    data class DescriptionBlock(
        @Json(name = "type") val type: BlockType,
        @Json(name = "title") val title: String?,
        @Json(name = "text") val text: String?,
        @Json(name = "items") val items: List<BlockItem>?,
        @Json(name = "headers") val headers: List<String>?,
        @Json(name = "columnTypes") val columnTypes: List<ValueType>?,
        @Json(name = "rows") val rows: List<List<TableCell>>?,
        @Json(name = "link") val link: Link?,
    )

    /** [value] is a number or a string depending on [valueType]; Moshi reads both as a string. */
    @JsonClass(generateAdapter = true)
    data class BlockItem(
        @Json(name = "label") val label: String,
        @Json(name = "value") val value: String,
        @Json(name = "valueType") val valueType: ValueType,
        @Json(name = "currency") val currency: String?,
    )

    @JsonClass(generateAdapter = true)
    data class TableCell(
        @Json(name = "text") val text: String?,
        @Json(name = "value") val value: BigDecimal?,
        @Json(name = "currency") val currency: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Link(
        @Json(name = "text") val text: String,
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