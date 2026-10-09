package com.tangem.datasource.api.marketing.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class MarketingCampaignsResponse(
    @Json(name = "campaigns") val campaigns: List<CampaignDto>,
)

@Serializable
@JsonClass(generateAdapter = true)
data class CampaignDto(
    @Json(name = "id") val id: Int,
    @Json(name = "type") val type: String,
    @Json(name = "priority") val priority: Int,
    @Json(name = "startAt") val startAt: String? = null,
    @Json(name = "endAt") val endAt: String? = null,
    @Json(name = "minAmount") val minAmount: SerializedBigDecimal? = null,
    @Json(name = "maxAmount") val maxAmount: SerializedBigDecimal? = null,
    @Json(name = "providerIds") val providerIds: List<String>? = null,
    @Json(name = "tokens") val tokens: List<CampaignTokenDto>? = null,
    @Json(name = "banner") val banner: BannerDto,
)

@Serializable
@JsonClass(generateAdapter = true)
data class CampaignTokenDto(
    @Json(name = "networkId") val networkId: String? = null,
    @Json(name = "contractAddress") val contractAddress: String? = null,
    @Json(name = "id") val id: String? = null,
)

@Serializable
@JsonClass(generateAdapter = true)
data class BannerDto(
    @Json(name = "uiType") val uiType: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "iconAlign") val iconAlign: String? = null,
    @Json(name = "bgColor") val bgColor: String? = null,
    @Json(name = "deeplink") val deeplink: String? = null,
    @SerialName("dismissible") @Json(name = "dismissible") val isDismissible: Boolean = false,
)