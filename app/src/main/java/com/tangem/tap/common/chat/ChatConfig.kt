package com.tangem.tap.common.chat

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed interface ChatConfig

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
) : ChatConfig

data class SprinklrConfig(
    val appId: String,
) : ChatConfig