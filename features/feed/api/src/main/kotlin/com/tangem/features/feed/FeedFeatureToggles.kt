package com.tangem.features.feed

interface FeedFeatureToggles {

    /** RWA world tab + Crypto category selector in the new feed (TWI-1477 MVP). */
    val isRwaAndCategoriesEnabled: Boolean
}