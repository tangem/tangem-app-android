package com.tangem.data.sociallogin.recovery

import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.social.SocialRecoveryError

internal fun SocialRecoveryError.toDomainError(): SocialLoginError = when (this) {
    SocialRecoveryError.BindingNotFound -> SocialLoginError.BindingNotFound
    SocialRecoveryError.BindingConflict -> SocialLoginError.BindingConflict
    // the backend has no dedicated code for an expired nonce, so it reads as any other server failure
    SocialRecoveryError.NonceExpired,
    SocialRecoveryError.NoActiveSession,
    SocialRecoveryError.DeviceKeyUnavailable,
    -> SocialLoginError.ServerError()
    is SocialRecoveryError.SigningFailed -> SocialLoginError.ServerError(cause)
    is SocialRecoveryError.DecryptionFailed -> SocialLoginError.RecoveryKeyDecryptionFailed(cause)
    is SocialRecoveryError.Api -> when (error) {
        AuthError.NetworkError -> SocialLoginError.NetworkError
        else -> SocialLoginError.ServerError()
    }
    is SocialRecoveryError.Unknown -> SocialLoginError.Unknown(cause)
}