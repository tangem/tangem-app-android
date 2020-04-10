package com.tangem.common

import com.tangem.SessionError
import com.tangem.common.CompletionResult.Success

/**
 * Response class encapsulating successful and failed results.
 * @param T Type of data that is returned in [Success].
 */
sealed class CompletionResult<T> {
    class Success<T>(val data: T) : CompletionResult<T>()
    class Failure<T>(val error: SessionError) : CompletionResult<T>()
}