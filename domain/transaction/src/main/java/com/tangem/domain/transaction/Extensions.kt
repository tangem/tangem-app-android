package com.tangem.domain.transaction

import arrow.core.raise.Raise
import com.tangem.domain.transaction.error.GetFeeError

fun Raise<GetFeeError>.raiseIllegalStateError(error: String): Nothing {
    raise(GetFeeError.DataError(IllegalStateException(error)))
}