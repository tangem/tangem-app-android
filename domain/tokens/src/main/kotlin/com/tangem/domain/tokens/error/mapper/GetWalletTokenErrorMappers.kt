package com.tangem.domain.tokens.error.mapper

import com.tangem.domain.tokens.error.TokenError
import com.tangem.domain.tokens.operations.TokensStatusesOperations

internal fun TokensStatusesOperations.Error.mapToTokenError(): TokenError {
    return when (this) {
        is TokensStatusesOperations.Error.DataError -> TokenError.DataError(this.cause)
        is TokensStatusesOperations.Error.EmptyNetworksStatuses,
        is TokensStatusesOperations.Error.EmptyQuotes,
        is TokensStatusesOperations.Error.EmptyTokens,
        -> TokenError.UnableToCreateToken
    }
}
