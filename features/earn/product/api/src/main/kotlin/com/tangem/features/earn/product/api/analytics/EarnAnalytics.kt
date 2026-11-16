package com.tangem.features.earn.product.api.analytics

import kotlinx.serialization.Serializable

data class EarnProductAnalytics(val category: String, val product: String)

/** Preliminary shape: the set of events is not final, so the parameters are an open map of strings. */
@Serializable
data class EarnTxAnalytics(val params: Map<String, String> = emptyMap())