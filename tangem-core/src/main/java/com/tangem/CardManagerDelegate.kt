package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.tasks.TaskError

interface CardManagerDelegate {

    fun onTaskStarted()
    fun showSecurityDelay(ms: Int)
    fun onTaskCompleted()
    fun onTaskError(error: TaskError? = null)

    fun requestPin(callback: (result: CompletionResult<String>) -> Unit)

}