package com.tangem.tap.common.zendesk

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Zendesk(
    val zendeskApiKey: String,
    val zendeskAppId: String,
    val zendeskClientId: String,
    val zendeskUrl: String,
)