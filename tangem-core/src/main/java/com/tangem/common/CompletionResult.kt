package com.tangem.common

import com.tangem.common.CompletionResult.Success
import com.tangem.tasks.TaskError

/**
 * Response class encapsulating successful and failed results.
 * [T] is a type of data that is returned in [Success].
 */
sealed class CompletionResult<T> {
    class Success<T>(val data: T) : CompletionResult<T>()
    class Failure<T>(val error: TaskError) : CompletionResult<T>()
}