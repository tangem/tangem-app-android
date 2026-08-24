package com.tangem.lib.auth.attestation

/**
 * Produces a platform device-integrity attestation token that accompanies the signed auth requests
 * (device register / authenticate / wallet register).
 *
 * Attestation is best-effort and optional: the backend accepts a `null` token, so implementations
 * MUST NOT throw. A missing platform capability (no Google Play services, a Huawei/HMS device) or a
 * failed platform call resolves to `null` and never blocks authentication.
 */
interface AttestationProvider {

    /**
     * Best-effort attestation token bound to [nonce], or `null` when attestation is unavailable.
     *
     * @param nonce the decrypted auth nonce the token is bound to, so the backend can tie the
     * attestation to this exact request.
     */
    suspend fun getAttestationToken(nonce: String): String?
}