package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

class DeleteSocialBackupAfterUpgradeUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Boolean> = either {
        if (!socialBackupRepository.isSocialBackupCompleted(walletId)) return@either false

        socialBackupRepository.deleteBackup(walletId).bind()
        socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false).bind()
        true
    }
}