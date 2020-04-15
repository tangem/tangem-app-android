package com.tangem.tangemtest._arch.structure

/**
[REDACTED_AUTHOR]
 */
interface Id

class StringId(val value: String) : Id

class StringResId(val value: Int) : Id

enum class Additional : Id {
    UNDEFINED,
    JSON_INCOMING,
    JSON_OUTGOING,
    JSON_TAILS,
}