package com.tangem.devkit.ucase.tunnel

import com.tangem.devkit._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */
interface Errors : Id

enum class CardError : Errors {
    NotPersonalized,
}

enum class ItemError : Errors {
    BadSeries,
    BadCardNumber,
}