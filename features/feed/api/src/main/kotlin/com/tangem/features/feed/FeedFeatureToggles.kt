package com.tangem.features.feed

interface FeedFeatureToggles {

    /** New Shtorka 2.0 — the redesigned feed replacing the legacy one. */
    val isNewShtorkaEnabled: Boolean

    /** RWA world tab + Crypto category selector in the new feed (TWI-1477 MVP). */
    val isRwaAndCategoriesEnabled: Boolean
}