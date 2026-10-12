package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.RestoredSocialBackup
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

/** Reads the social backup from the cloud and decrypts it with the recovery key. */
class ReadSocialBackupUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(recoveryKey: RecoveryKey): Either<SocialLoginError, RestoredSocialBackup> {
        return socialBackupRepository.readBackup(recoveryKey)
    }
}