package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

/**
 * For the restore path, where both components already exist and only this device has yet to learn about
 * them. The create path uses [RegisterSocialRecoveryUseCase], which sets the flag as its last step.
 */
class SetSocialBackupCompletedUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId, isCompleted: Boolean): Either<SocialLoginError, Unit> =
        socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = isCompleted)
}