package com.tangem.lib.auth.session

import kotlinx.datetime.Instant

/**
 * JWT session tokens issued by the Tangem Auth Service — pure domain model.
 *
 * Persisted on the device via [SessionTokensStore]. The default implementation serialises a
 * storage DTO mirroring the wire format (`TokenApiResponse`) into AES-256-GCM-encrypted local
 * storage (`SecureStorage` / `AndroidSecureStorageV2`) with the master key residing in
 * AndroidKeystore. Survives app process death but not user data wipe / app uninstall.
 *
 * The domain class itself carries no serialization annotations: it can grow with business
 * helpers (`isAccessTokenExpired`, computed properties, etc.) without touching the on-disk
 * format.
 *
 * @property accessToken short-lived signed JWT (verified via JWKS at API Gateway). Sent as
 *   `Authorization: DPoP <accessToken>` on every authenticated request.
 * @property refreshToken opaque rotation token. `null` for ORANGE-tier sessions (require full
 *   re-authentication for every new access token — see SR-8 / token policy by trust tier).
 * @property refreshTokenExpiresAt `null` if [refreshToken] is `null`.
 * @property walletIds wallet ids bound to the device by the backend, mirrored from token claims
 *   to avoid parsing the JWT on the client.
 */
data class SessionTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String?,
    val refreshTokenExpiresAt: Instant?,
    val walletIds: List<String>,
)