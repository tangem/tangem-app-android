package com.tangem.core.biometric.impl

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.tangem.core.biometric.BiometricAuthError
import com.tangem.core.biometric.BiometricAuthManager
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

internal class DefaultBiometricAuthManager @Inject constructor(
    private val foregroundActivityProvider: ForegroundActivityProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : BiometricAuthManager {

    override suspend fun authenticate(config: BiometricAuthManager.Config): BiometricAuthManager.Result =
        withContext(dispatchers.main) {
            val activity = foregroundActivityProvider.foregroundActivity
            if (activity == null ||
                !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
                activity.supportFragmentManager.isStateSaved
            ) {
                return@withContext BiometricAuthManager.Result.Failure(
                    error = BiometricAuthError.NoForegroundActivity,
                    message = "Host activity is not ready to show the prompt",
                )
            }

            suspendCancellableCoroutine<BiometricAuthManager.Result> { continuation ->
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(BiometricAuthManager.Result.Success)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!continuation.isActive) return
                        continuation.resume(errorCode.toAuthResult(message = errString.toString()))
                    }
                }

                val promptInfo = PromptInfo.Builder()
                    .setTitle(config.title.resolveReference(activity.resources))
                    .apply {
                        val subtitle = config.subtitle
                        if (subtitle != null) {
                            setSubtitle(subtitle.resolveReference(activity.resources))
                        }
                    }
                    .setAllowedAuthenticators(
                        if (isStrongWithCredentialSupported()) {
                            Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
                        } else {
                            Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
                        },
                    )
                    .build()

                val mainExecutor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(activity, mainExecutor, callback)
                continuation.invokeOnCancellation { mainExecutor.execute(prompt::cancelAuthentication) }
                prompt.authenticate(promptInfo)
            }
        }

    private fun Int.toAuthResult(message: String): BiometricAuthManager.Result = when (this) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        -> BiometricAuthManager.Result.Cancelled

        else -> BiometricAuthManager.Result.Failure(error = toAuthError(), message = message)
    }

    private fun Int.toAuthError(): BiometricAuthError = when (this) {
        BiometricPrompt.ERROR_HW_UNAVAILABLE,
        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        -> BiometricAuthError.HardwareUnavailable

        BiometricPrompt.ERROR_CANCELED -> BiometricAuthError.SystemCancelled
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> BiometricAuthError.NoDeviceCredential
        BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricAuthError.NoBiometricEnrolled
        BiometricPrompt.ERROR_LOCKOUT -> BiometricAuthError.LockedOut
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricAuthError.LockedOutPermanently
        BiometricPrompt.ERROR_TIMEOUT -> BiometricAuthError.Timeout
        BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAuthError.SecurityUpdateRequired
        else -> BiometricAuthError.Unknown
    }
}

private fun isStrongWithCredentialSupported(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.P || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q