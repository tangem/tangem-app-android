package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import javax.inject.Inject

/**
 * Tears down the social recovery of a wallet — drops the backend binding, clears the local completion
 * flag and deletes the cloud file. Used after an upgrade to a hardware wallet, so that the social
 * account can no longer restore the wallet it used to own (FR-16).
 *
 * Unbinding comes first because it is what actually enforces FR-16: without the recovery key the cloud
 * file is unreadable, while deleting the file guarantees nothing on its own — it may already have been
 * copied. It also frees the identity, which FR-14 would otherwise keep occupied by a dead binding, and
 * it does not depend on the cloud being reachable. The flag follows immediately: from that point the
 * wallet is not recoverable and must stop counting as backed up (FR-15). Deleting the leftover file goes
 * last — an encrypted file whose key no longer exists is harmless.
 *
 * Forgetting a wallet does not go through here: it leaves social recovery intact (FR-11).
 */
class DeleteSocialRecoveryUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
    private val socialRecoveryRepository: SocialRecoveryRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Unit> = either {
        socialRecoveryRepository.unbindWallet(walletId).bind()
        socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false).bind()
        socialBackupRepository.deleteBackup(walletId).bind()
    }
}