package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

class RevokeSocialRecoveryUseCase @Inject constructor(
    private val socialRecoveryRepository: SocialRecoveryRepository,
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Unit> = either {
        socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false).bind()

        socialRecoveryRepository.unbindWallet(walletId)
            .onLeft { socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = true) }
            .bind()

        socialBackupRepository.deleteBackup(walletId)
    }
}