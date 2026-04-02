package com.tangem.datasource.api.tangemTech.models.promobanners

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromoBannerDisplaysResponse(
    @Json(name = "items")
    val items: List<PromoBannerDisplayDTO>,
)

@Suppress("BooleanPropertyNaming")
@JsonClass(generateAdapter = true)
data class PromoBannerDisplayDTO(
    @Json(name = "id")
    val id: Int,
    @Json(name = "placeholder")
    val placeholder: String,
    @Json(name = "priority")
    val priority: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "subtitle")
    val subtitle: String,
    @Json(name = "iconUrl")
    val iconUrl: String?,
    @Json(name = "deeplink")
    val deeplink: String?,
    @Json(name = "buttonEnabled")
    val buttonEnabled: Boolean,
    @Json(name = "buttonText")
    val buttonText: String?,
    @Json(name = "dismissable")
    val dismissable: Boolean,
)