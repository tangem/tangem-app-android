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
     * Stable, newline-separated representation of the signed payload. Backend treats the bytes
     * opaquely; must stay aligned with the server-side canonicalisation. Field order matches the
     * declaration order of [RegisterPayload] / [AuthenticationPayload] and [DeviceMetadata].
     */
    private fun canonicalize(
        devicePublicKey: String,
        nonce: String,
        attestationToken: String?,
        metadata: DeviceMetadata,
    ): ByteArray = buildString {
        append(devicePublicKey).append('\n')
        append(nonce).append('\n')
        append(attestationToken.orEmpty()).append('\n')
        append(metadata.deviceModel).append('\n')
        append(metadata.os).append('\n')
        append(metadata.osVersion).append('\n')
        append(metadata.appVersion).append('\n')
        append(metadata.userAgent).append('\n')
        append(metadata.locale).append('\n')
        append(metadata.timezone)
    }.toByteArray(Charsets.UTF_8)
}

/** Base64-encodes [this] without line wraps — required for DPoP proofs and device signatures. */
internal fun ByteArray.toBase64NoWrap(): String = Base64.encodeToString(this, Base64.NO_WRAP)