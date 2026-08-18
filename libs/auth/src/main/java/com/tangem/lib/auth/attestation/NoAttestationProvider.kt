package com.tangem.lib.auth.attestation

/**
 * No-op [AttestationProvider] that always yields `null`. Bound on platforms without a supported
 * attestation backend — Huawei / devices without Google Play services.
 */
object NoAttestationProvider : AttestationProvider {

    override suspend fun getAttestationToken(nonce: String): String? = null
}