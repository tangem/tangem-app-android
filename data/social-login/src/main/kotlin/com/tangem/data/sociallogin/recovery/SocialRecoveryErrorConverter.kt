package com.tangem.data.sociallogin.recovery

import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.social.SocialRecoveryError

internal fun SocialRecoveryError.toDomainError(): SocialLoginError = when (this) {
    SocialRecoveryError.BindingNotFound -> SocialLoginError.BindingNotFound
    SocialRecoveryError.BindingConflict -> SocialLoginError.BindingConflict
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