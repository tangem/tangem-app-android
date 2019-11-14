package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.tasks.TaskError

/**
 * Allows interaction with users and shows visual elements.
 *
 * Its default implementation, DefaultCardManagerDelegate, is in our tangem-sdk module.
 */
interface CardManagerDelegate {

    fun onNfcSessionStarted()
    fun onSecurityDelay(ms: Int)
    fun onTagLost()
    fun onNfcSessionCompleted()
    fun onError(error: TaskError? = null)

    fun requestPin(callback: (result: CompletionResult<String>) -> Unit)

}