package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.tasks.TaskError

interface CardManagerDelegate {

    fun onNfcSessionStarted()
    fun onSecurityDelay(ms: Int)
    fun hideSecurityDelay()
    fun onNfcSessionCompleted()
    fun onError(error: TaskError? = null)

    fun requestPin(callback: (result: CompletionResult<String>) -> Unit)

}