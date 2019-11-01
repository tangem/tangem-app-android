package com.tangem.common

import com.tangem.tasks.TaskError

sealed class CompletionResult<T> {
    class Success<T>(val data: T) : CompletionResult<T>()
    class Failure<T>(val error: TaskError) : CompletionResult<T>()
}