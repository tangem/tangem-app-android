package com.tangem

import com.tangem.tasks.TaskError

interface CardManagerDelegate {

    fun showSecurityDelay(ms: Int)
    fun hideSecurityDelay()
    fun requestPin(success: () -> String, error: (cardError: TaskError.CardError) -> TaskError.CardError)

    fun openNfcPopup()
    fun closeNfcPopup()

}