package com.tangem.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents the type of a group of tokens
 *
[REDACTED_AUTHOR]
 */
@Serializable
enum class TokensGroupType {

    /** No grouping applied */
    NONE,

    /** Grouping by network */
    NETWORK,
    ;
}