package com.tangem.tap.features.root

/**
 * Resumes the startup gate after the root-detected security warning is dismissed.
 *
 * The gate awaits [awaitDismiss] while the warning is shown, and the screen calls [dismiss] on "Continue".
 */
interface RootWarningContinuation {

    suspend fun awaitDismiss()

    fun dismiss()
}