package com.tangem.lib.auth.session.internal

import android.util.Base64
import com.tangem.datasource.api.auth.models.request.AuthenticationPayload
import com.tangem.datasource.api.auth.models.request.DeviceMetadata
import com.tangem.datasource.api.auth.models.request.RegisterPayload
import com.tangem.utils.info.AppInfoProvider
import javax.inject.Inject

/**
 * Shared helpers for the device-signed request payloads used by `/auth/register`
 * ([RegisterPayload]) and `/auth/authenticate` ([AuthenticationPayload]). The two DTOs have
 * identical field shapes — the canonicalisation is parameterised by primitives and exposed via
 * type-specific overloads, so the two DTOs don't need to share a common interface.
 */
internal class SignedRequestPayload @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
) {

    /** Snapshot of [appInfoProvider]'s device facts as the network DTO. */
    val deviceMetadata: DeviceMetadata
        get() = DeviceMetadata(
            deviceModel = appInfoProvider.device,
            // Backend contract is lowercase `android`/`ios`; AppInfoProvider returns `"Android"`.
            os = appInfoProvider.platform.lowercase(),
            osVersion = appInfoProvider.osVersion,
            appVersion = appInfoProvider.appVersion,
            userAgent = with(appInfoProvider) { "Tangem/$appVersion ($device; $platform $osVersion)" },
            locale = appInfoProvider.language,
            timezone = appInfoProvider.timezone,
        )

    /** @see canonicalize */
    fun canonicalize(payload: AuthenticationPayload): ByteArray = canonicalize(
        devicePublicKey = payload.devicePublicKey,
        nonce = payload.nonce,
        attestationToken = payload.attestationToken,
        metadata = payload.metadata,
    )

    /** @see canonicalize */
    fun canonicalize(payload: RegisterPayload): ByteArray = canonicalize(
        devicePublicKey = payload.devicePublicKey,
        nonce = payload.nonce,
        attestationToken = payload.attestationToken,
        metadata = payload.metadata,
    )

    /**
     * Stable, colon-separated representation of the signed payload. Must stay byte-for-byte aligned
     * with the server-side canonicalisation (`RegistrationService` / `AuthenticationService`):
     * fields joined with `:` in this exact order, with a missing `attestationToken` collapsing to
     * an empty segment, and no trailing separator. The server verifies via `SHA256withECDSA` over
     * these UTF-8 bytes.
     */
    private fun canonicalize(
        devicePublicKey: String,
        nonce: String,
        attestationToken: String?,
        metadata: DeviceMetadata,
    ): ByteArray = buildString {
        append(devicePublicKey).append(':')
        append(nonce).append(':')
        append(attestationToken.orEmpty()).append(':')
        append(metadata.deviceModel).append(':')
        append(metadata.os).append(':')
        append(metadata.osVersion).append(':')
        append(metadata.appVersion).append(':')
        append(metadata.userAgent).append(':')
        append(metadata.locale).append(':')
        append(metadata.timezone)
    }.toByteArray(Charsets.UTF_8)
}

/** Base64-encodes [this] without line wraps — required for DPoP proofs and device signatures. */
internal fun ByteArray.toBase64NoWrap(): String = Base64.encodeToString(this, Base64.NO_WRAP)