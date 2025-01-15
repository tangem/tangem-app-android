package com.tangem.datasource.local.config.providers.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Config provider model */
sealed class ProviderModel {

    /**
     * Example,
     * {
     *    "type": "public",
     *    "url": "https://example.com"
     * }
     */
    @JsonClass(generateAdapter = true)
    data class Public(
        @Json(name = "url") val url: String,
    ) : ProviderModel()

    /**
     * Example,
     * {
     *    "type": "private",
     *    "name": "nownodes"
     * }
     */
    @JsonClass(generateAdapter = true)
    data class Private(
        @Json(name = "name") val name: String,
    ) : ProviderModel()

    /** Unsupported type */
    data object UnsupportedType : ProviderModel()
}