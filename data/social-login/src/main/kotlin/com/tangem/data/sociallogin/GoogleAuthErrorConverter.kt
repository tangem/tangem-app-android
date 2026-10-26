package com.tangem.data.sociallogin

import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.google.auth.GoogleAuthError

internal fun GoogleAuthError.toDomainError(): SocialLoginError = when (this) {
    // kept out of [SocialLoginError.CloudUnavailable] on purpose: that one is what the silent retry keys
    // on, and asking again for a grant the viewer just declined can only fail a second time
    GoogleAuthError.AuthCanceled -> SocialLoginError.SignInCanceled
    GoogleAuthError.AuthRequired,
    GoogleAuthError.PermissionsMissing,
    GoogleAuthError.Unavailable,
    -> SocialLoginError.CloudUnavailable
    GoogleAuthError.NetworkError -> SocialLoginError.NetworkError
    is GoogleAuthError.Unknown -> SocialLoginError.Unknown(cause)
}