package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

/**
 * Binds a wallet to a provider identity and stores its recovery key with the backend.
 *
 * Does not mark the wallet as socially backed up: the cloud file comes after, and until it is there the
 * wallet is bound but not restorable (NFR-01). Setting the flag is the caller's.
 */
class RegisterSocialRecoveryUseCase @Inject constructor(
    private val socialRecoveryRepository: SocialRecoveryRepository,
) {

    suspend operator fun invoke(
        idToken: SocialProviderIdToken,
        walletId: UserWalletId,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit> = socialRecoveryRepository.registerRecoveryKey(
        idToken = idToken,
        walletId = walletId,
        recoveryKey = recoveryKey,
    )
}