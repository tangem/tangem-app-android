package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.sociallogin.models.SocialBackupInfo
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

/**
 * The header is unauthenticated — only [ReadSocialBackupUseCase] verifies it against the recovery key —
 * so use it to decide which file to touch or to report what is stored, never to trust the wallet id.
 */
class GetSocialBackupInfoUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(): Either<SocialLoginError, SocialBackupInfo> = socialBackupRepository.getBackupInfo()
}