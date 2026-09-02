package com.tangem.domain.sociallogin.crypto

import com.tangem.domain.sociallogin.models.RecoveryKey

/**
 * Source of fresh recovery keys — 32 bytes from a CSPRNG, used directly as the AES-256 key (CPR-01).
 *
 * Implemented and bound in the data layer together with the backup cipher; until then nothing may
 * inject [com.tangem.domain.sociallogin.usecase.GenerateRecoveryKeyUseCase], as the graph has no
 * binding for it.
 */
interface RecoveryKeyGenerator {

    fun generate(): RecoveryKey
}