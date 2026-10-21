package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

class RevokeSocialRecoveryUseCase @Inject constructor(
    private val socialRecoveryRepository: SocialRecoveryRepository,
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(userWallet: UserWallet.Hot): Either<SocialLoginError, Unit> = either {
        val walletId = userWallet.walletId
        socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false).bind()

        socialRecoveryRepository.unbindWallet(userWallet)
            .onLeft { socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = true) }
            .bind()

        socialBackupRepository.deleteBackup(walletId)
    }
}