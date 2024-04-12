package com.tangem.datasource.config.models

import com.squareup.moshi.Json

/** Config provider model */
sealed class ProviderModel {

    /**
     * Example,
     * {
     *    "type": "public",
     *    "url": "https://example.com"
     * }
     */
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
    data class Private(
        @Json(name = "name") val name: String,
    ) : ProviderModel()

    /** Unsupported type */
    data object UnsupportedType : ProviderModel()
}