package com.tangem.datasource.api.marketing.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Cached campaigns response plus its ETag, persisted per [CampaignDto.type] for revalidation. */
@JsonClass(generateAdapter = true)
data class MarketingCampaignsCacheEntry(
    @Json(name = "eTag") val eTag: String?,
    @Json(name = "response") val response: MarketingCampaignsResponse,
)