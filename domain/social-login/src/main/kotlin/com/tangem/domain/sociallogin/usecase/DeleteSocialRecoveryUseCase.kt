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
 * The local flag is cleared first, since it is the one step that can still be taken back. Unbinding is
 * what enforces FR-16 — it destroys the key — and the file is removed even when that fails, because a
 * file that is gone cannot be restored from either.
 *
 * Forgetting a wallet does not go through here: it leaves social recovery intact (FR-11).
 */
class DeleteSocialRecoveryUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
    private val socialRecoveryRepository: SocialRecoveryRepository,
) {

    suspend operator fun invoke(walletId: UserWalletId): Either<SocialLoginError, Unit> = either {
        // The stored flag, not IsSocialBackupCompletedUseCase: that one reports `false` while the
        // feature toggle is off, which would skip the teardown and leave the binding alive.
        val wasCompleted = socialBackupRepository.isSocialBackupCompleted(walletId)

        // Ahead of the destructive part, and the only step allowed to abort it: a flag that cannot be
        // stored would otherwise survive the key it describes and keep a wallet with no recovery left
        // counting as backed up. Nothing to outlive anything when it is already false, and insisting on
        // the write there would only keep a half-registered binding alive over a local storage failure.
        if (wasCompleted) {
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false).bind()
        }

        // Attempted whatever the flag said, because a binding can outlive it — registration stores the
        // binding first.
        val unbindError = socialRecoveryRepository.unbindWallet(walletId).fold(ifLeft = { it }, ifRight = { null })

        // The rest is for a wallet that finished its setup: the others were upgrading a wallet social
        // login never touched, and a failed unbind there says nothing worth failing the upgrade over.
        if (!wasCompleted) return@either

        // Runs even when the unbind did not: the backend may be unreachable, or its authentication
        // switched off entirely, and a wallet with no file has nothing left to restore from either.
        // Guarded by the flag because even a silent Drive authorization is a network round-trip that
        // every upgrade to a card would otherwise pay for.
        val deleteError = socialBackupRepository.deleteBackup(walletId).fold(ifLeft = { it }, ifRight = { null })

        // The unbind error wins: a live binding is what the caller has to know about, a leftover file
        // without it is the lesser problem.
        (unbindError ?: deleteError)?.let { raise(it) }
    }
}