package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

/**
 * Deletes the cloud file only: the upgrade has already replaced the hot wallet's record, and its social
 * backup mark went with it. The caller decides whether there is a backup, before the record is replaced,
 * so a missing file is [SocialLoginError.BackupNotFound] rather than a success.
 */
class DeleteSocialBackupAfterUpgradeUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Unit> = either {
        val isDeleted = socialBackupRepository.deleteBackup(walletId).bind()
        ensure(isDeleted) { SocialLoginError.BackupNotFound }
    }
}