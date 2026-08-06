package com.tangem.core.biometric

import com.tangem.core.ui.extensions.TextReference

/**
 * Shows a system authentication prompt (biometrics and/or device credential) as a gate before a
 * sensitive action.
 */
interface BiometricAuthManager {

    suspend fun authenticate(config: Config): Result

    /**
     * @param title text shown as the prompt title.
     * @param subtitle optional text shown as the prompt subtitle.
     */
    data class Config(
        val title: TextReference,
        val subtitle: TextReference?,
    )

    sealed interface Result {
        data object Success : Result

        /**
         * The user dismissed the prompt: cancel, back or the negative button. Not a failure — the
         * guarded action is simply not performed, and there is nothing to report.
         */
        data object Cancelled : Result

        /**
         * @param error what went wrong, see [BiometricAuthError].
         * @param message system-provided description of the error, for logging only — the system has
         * already shown it to the user.
         */
        data class Failure(val error: BiometricAuthError, val message: String?) : Result
    }
}