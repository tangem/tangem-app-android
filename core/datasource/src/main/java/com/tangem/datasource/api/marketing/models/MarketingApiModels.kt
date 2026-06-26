package com.tangem.datasource.api.marketing.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class MarketingCampaignsResponse(
    @Json(name = "campaigns") val campaigns: List<CampaignDto>,
)

@JsonClass(generateAdapter = true)
data class CampaignDto(
    @Json(name = "id") val id: Int,
    @Json(name = "type") val type: String,
    @Json(name = "priority") val priority: Int,
    @Json(name = "minAmount") val minAmount: BigDecimal? = null,
    @Json(name = "maxAmount") val maxAmount: BigDecimal? = null,
    @Json(name = "providerIds") val providerIds: List<String>? = null,
    @Json(name = "tokens") val tokens: List<CampaignTokenDto>? = null,
    @Json(name = "banner") val banner: BannerDto,
)

@JsonClass(generateAdapter = true)
data class CampaignTokenDto(
    @Json(name = "networkId") val networkId: String? = null,
    @Json(name = "contractAddress") val contractAddress: String? = null,
    @Json(name = "id") val id: String? = null,
)

@JsonClass(generateAdapter = true)
data class BannerDto(
    @Json(name = "uiType") val uiType: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "iconAlign") val iconAlign: String? = null,
    @Json(name = "bgColor") val bgColor: String? = null,
    @Json(name = "deeplink") val deeplink: String? = null,
    @Json(name = "dismissible") val isDismissible: Boolean = false,
)

/** Cached campaigns response plus its ETag, persisted per [CampaignDto.type] for revalidation. */
@JsonClass(generateAdapter = true)
data class MarketingCampaignsCacheEntry(
    @Json(name = "eTag") val eTag: String?,
    @Json(name = "response") val response: MarketingCampaignsResponse,
)