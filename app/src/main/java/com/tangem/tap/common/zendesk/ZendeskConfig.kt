package com.tangem.tap.common.zendesk

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ZendeskConfig(
    @Json(name = "zendeskApiKey")
    val apiKey: String,
    @Json(name = "zendeskAppId")
    val appId: String,
    @Json(name = "zendeskClientId")
    val clientId: String,
    @Json(name = "zendeskAccountKey")
    val accountKey: String,
    @Json(name = "zendeskUrl")
    val url: String,
)