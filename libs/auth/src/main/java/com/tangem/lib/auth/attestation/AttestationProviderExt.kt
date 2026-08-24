package com.tangem.lib.auth.attestation

import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger

/**
 * Best-effort wrapper over [AttestationProvider.getAttestationToken]: any failure is logged and
 * collapsed to `null`, so attestation never blocks authentication even if an implementation
 * violates the no-throw contract.
 */
internal suspend fun AttestationProvider.getAttestationTokenOrNull(nonce: String): String? =
    runSuspendCatching { getAttestationToken(nonce) }
        .onFailure { TangemLogger.e("Attestation token retrieval failed; proceeding without it", it) }
        .getOrNull()