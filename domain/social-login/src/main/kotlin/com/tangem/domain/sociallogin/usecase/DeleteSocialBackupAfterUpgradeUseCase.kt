package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

/**
 * Deletes the cloud file only: the upgrade has already replaced the hot wallet's record, and its social
 * backup mark went with it. The caller decides whether there is a backup, before the record is replaced.
 */
class DeleteSocialBackupAfterUpgradeUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Unit> =
        socialBackupRepository.deleteBackup(walletId)
}