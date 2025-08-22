package com.tangem.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents the sorting type for tokens
 *
[REDACTED_AUTHOR]
 */
@Serializable
enum class TokensSortType {

    /** No sorting applied */
    NONE,

    /** Sorted by their balance */
    BALANCE,
    ;
}