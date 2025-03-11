package com.tangem.lib.crypto.models

/**
 * Analytics data for send events in analytics engine
 *
 * @property feeType type of fee (min,max,normal)
 * @property tokenSymbol symbol
 * @property permissionType optional parameter used for type tx approve
 */
data class AnalyticsData(
    val feeType: String,
    val tokenSymbol: String,
    val permissionType: String? = null,
)