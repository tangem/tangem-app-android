package com.tangem.domain.sociallogin.models

/**
 * Only Google: Apple sign-in has no Android counterpart. The enum still exists because identity,
 * availability and analytics are provider-scoped on the backend.
 */
enum class SocialLoginProvider {
    Google,
}