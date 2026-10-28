package com.tangem.store.datasource.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Where the request comes from: a dApp [domain], or [isNonDapp] for a transaction the wallet built itself
 */
@JsonClass(generateAdapter = true)
data class TransactionMetadata(
    @Json(name = "domain") val domain: String? = null,
    @Json(name = "non_dapp") val isNonDapp: Boolean? = null,
)