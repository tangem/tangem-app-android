package com.tangem.features.forceupdate

/**
 * Resumes the regular app startup after an optional update screen is dismissed.
 *
 * The startup awaits [awaitDismiss] while the optional update is shown, and the screen calls
 * [dismiss] on "Later" to let the normal startup (and its side effects) run afterwards.
 */
interface ForceUpdateContinuation {

    suspend fun awaitDismiss()

    fun dismiss()
}