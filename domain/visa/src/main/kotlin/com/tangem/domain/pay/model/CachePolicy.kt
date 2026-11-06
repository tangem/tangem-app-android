package com.tangem.domain.pay.model

enum class CachePolicy {

    /** Returns only the cached value and never goes to the network; `null` when nothing is cached yet. */
    ONLY_CACHE,

    /** Returns the cached value when present, otherwise fetches it from the network and caches it. */
    CACHE_FIRST,

    /** Always fetches a fresh value from the network and updates the cache with it. */
    REFRESH,
}