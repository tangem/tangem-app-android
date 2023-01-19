package com.tangem.domain.common.extensions

import com.tangem.common.CompletionResult
import com.tangem.common.services.Result

/**
 * Created by Anton Zhilenkov on 07.10.2022.
 */
inline fun <T> Result<T>.successOr(failureClause: (Result.Failure) -> T): T {
    return when (this) {
        is Result.Success -> this.data
        is Result.Failure -> failureClause(this)
    }
}

inline fun <T> CompletionResult<T>.successOr(failureClause: (CompletionResult.Failure<T>) -> T): T {
    return when (this) {
        is CompletionResult.Success -> this.data
        is CompletionResult.Failure -> failureClause(this)
    }
}
