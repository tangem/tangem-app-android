package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

class RegisterRestoredWalletUseCase @Inject constructor(
    private val socialRecoveryRepository: SocialRecoveryRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Unit> =
        socialRecoveryRepository.registerRestoredWallet(walletId)
}