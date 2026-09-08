package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

/**
 * Resolves what a social login leads to: the recovery key of an existing binding, or
 * [SocialLoginError.BindingNotFound] telling the caller to create a wallet instead (FR-02).
 */
class GetSocialRecoveryKeyUseCase @Inject constructor(
    private val socialRecoveryRepository: SocialRecoveryRepository,
) {

    suspend operator fun invoke(idToken: SocialProviderIdToken): Either<SocialLoginError, RecoveryKey> {
        return socialRecoveryRepository.getRecoveryKey(idToken)
    }
}