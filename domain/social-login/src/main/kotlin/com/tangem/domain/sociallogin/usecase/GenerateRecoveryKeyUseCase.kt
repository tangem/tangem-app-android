package com.tangem.domain.sociallogin.usecase

import com.tangem.domain.sociallogin.crypto.RecoveryKeyGenerator
import com.tangem.domain.sociallogin.models.RecoveryKey
import javax.inject.Inject

/**

 * registered on the backend, so the create flow holds on to it until both have succeeded (NFR-01).
 */
class GenerateRecoveryKeyUseCase @Inject constructor(
    private val recoveryKeyGenerator: RecoveryKeyGenerator,
) {

    operator fun invoke(): RecoveryKey = recoveryKeyGenerator.generate()
}