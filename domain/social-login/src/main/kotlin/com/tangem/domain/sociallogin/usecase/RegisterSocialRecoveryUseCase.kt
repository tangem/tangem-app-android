package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

/**
 * Completes the social recovery setup of a wallet whose backup file is already in the cloud: binds the
 * wallet to the provider identity, stores the recovery key with it and only then marks the wallet as
 * socially backed up — a half-finished setup must never show up as one (NFR-01).
 */
class RegisterSocialRecoveryUseCase @Inject constructor(
    private val socialRecoveryRepository: SocialRecoveryRepository,
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(
        idToken: SocialProviderIdToken,
        walletId: UserWalletId,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit> = either {
        socialRecoveryRepository.registerRecoveryKey(
            idToken = idToken,
            walletId = walletId,
            recoveryKey = recoveryKey,
        ).bind()

        socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = true).bind()
    }
}