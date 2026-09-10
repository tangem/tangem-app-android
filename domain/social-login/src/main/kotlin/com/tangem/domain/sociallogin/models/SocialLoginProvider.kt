package com.tangem.domain.sociallogin.models

/**
 * Identity provider a social-login wallet can be bound to.
 *
 * FR-01 names Google and Apple, but Apple has no Android counterpart, so [Google] is the only member
 * here. The enum still exists because provider identity, availability (FR-13) and analytics are all
 * provider-scoped on the backend, and adding a provider must not reshape those contracts.
 */
enum class SocialLoginProvider {
    Google,
}