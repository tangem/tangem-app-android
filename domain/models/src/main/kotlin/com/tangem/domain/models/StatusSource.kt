package com.tangem.domain.models

/**
 * Source of the status of any loaded data
 *
* [REDACTED_AUTHOR]
 */
enum class StatusSource {

    /**
     * Status is loaded from the cache.
     * In most cases, it's a temporary value, then the source should become either [ACTUAL] or [ONLY_CACHE]
     */
    CACHE,

    /**
     * Status is updated by a data source that is of actual value.
     * In most cases, this is a remote data source. But it can also be a value that we have updated programmatically.
     */
    ACTUAL,

    /** Status is loaded from the cache and can't be updated with actual value */
    ONLY_CACHE,

    ;

    fun isActual() = this == ACTUAL
}

/**
 * Get result status source for the list of [StatusSource]
 *
 * ACTUAL, ACTUAL, ACTUAL     -> ACTUAL
 * ACTUAL, ACTUAL, CACHE      -> CACHE
 * ACTUAL, ACTUAL, ONLY_CACHE -> ONLY_CACHE
 * ACTUAL, CACHE,  ONLY_CACHE -> ONLY_CACHE
 */
fun List<StatusSource>.getResultStatusSource(): StatusSource {
    return when {
        any { it == StatusSource.ONLY_CACHE } -> StatusSource.ONLY_CACHE
        any { it == StatusSource.CACHE } -> StatusSource.CACHE
        else -> StatusSource.ACTUAL
    }
}
