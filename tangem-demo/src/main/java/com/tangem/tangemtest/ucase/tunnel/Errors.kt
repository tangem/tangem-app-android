package com.tangem.tangemtest.ucase.tunnel

import com.tangem.tangemtest._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */
interface Errors: Id

enum class CardError : Errors {
    NotPersonalized,
}

enum class ItemError: Errors {
    BadSeries,
    BadCardNumber,
}