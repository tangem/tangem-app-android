package com.tangem.data.sociallogin.provider

import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.google.auth.GoogleIdTokenError
import com.tangem.utils.converter.Converter

internal object GoogleIdTokenErrorConverter : Converter<GoogleIdTokenError, SocialLoginError> {

    override fun convert(value: GoogleIdTokenError): SocialLoginError = when (value) {
        GoogleIdTokenError.Canceled -> SocialLoginError.SignInCanceled
        GoogleIdTokenError.NoCredential -> SocialLoginError.NoProviderAccount
        GoogleIdTokenError.Unavailable -> SocialLoginError.ProviderUnavailable
        is GoogleIdTokenError.Unknown -> SocialLoginError.SignInFailed(value.cause)
    }
}