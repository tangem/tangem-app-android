package com.tangem.domain.sociallogin.models

/**
 * Only Google: Apple sign-in has no Android counterpart. The enum still exists because identity,
 * availability and analytics are provider-scoped on the backend.
 *
 * The names are brand names, so they are not localized.
 */
enum class SocialLoginProvider(val displayName: String, val cloudName: String) {
    Google(displayName = "Google", cloudName = "Google Drive"),
}