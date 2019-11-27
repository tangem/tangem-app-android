package com.tangem

import com.tangem.common.CompletionResult
import com.tangem.tasks.TaskError

/**
 * Allows interaction with users and shows visual elements.
 *
 * Its default implementation, DefaultCardManagerDelegate, is in our tangem-sdk module.
 */
interface CardManagerDelegate {

    /**
     * It is called when user is expected to scan a Tangem Card with an Android device.
     */
    fun onNfcSessionStarted()

    /**
     * It is called when security delay is triggered by the card.
     * A user is expected to hold the card until the security delay is over.
     */
    fun onSecurityDelay(ms: Int)

    /**
     * It is called when user takes the card away from the Android device during the scanning
     * (for example when security delay is in progress) and the TagLostException is received.
     */
    fun onTagLost()

    /**
     * It is called when NFC session was completed and a user can take the card away from the Android device.
     */
    fun onNfcSessionCompleted()

    /**
     * It is called when some error occur during NFC session.
     */
    fun onError(error: TaskError? = null)

    /**
     * It is called when a user is expected to enter pin code.
     */
    fun onPinRequested(callback: (result: CompletionResult<String>) -> Unit)

}