package com.tangem.domain.sociallogin.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import javax.inject.Inject

/**
 * **Precondition: the binding of the signed-in identity must already be resolved** — either absent, or
 * pointing at [walletId]. Writing is destructive while registering the key can still be refused, so a
 * write started before the binding is known can destroy a backup that gets no replacement. The create
 * path satisfies this by entering only after the recover call answered 404.
 *
 * The folder holds one backup per cloud account, so a write can take the backup away from another
 * wallet. Its flag is cleared first, before the write: a wallet that still counted as backed up would
 * hide the backup prompts, while clearing too eagerly only over-prompts. An unreadable header names no
 * wallet to fix and is no obstacle; a header that failed to *arrive* aborts the write, since the file
 * it describes may belong to a wallet that would be left claiming a backup.
 *
 * This is half of the setup — social recovery counts as complete once the key is also registered, so
 * the written wallet is not marked as backed up here.
 */
class WriteSocialBackupUseCase @Inject constructor(
    private val socialBackupRepository: SocialBackupRepository,
) {

    suspend operator fun invoke(
        walletId: UserWalletId,
        secret: SocialBackupSecret,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit> = either {
        val replacedWalletId = socialBackupRepository.getBackupInfo().fold(
            ifLeft = { error -> if (error.namesNoWallet()) null else raise(error) },
            ifRight = { it.walletId },
        )

        if (replacedWalletId != null && replacedWalletId != walletId) {
            socialBackupRepository.setSocialBackupCompleted(walletId = replacedWalletId, isCompleted = false).bind()
        }

        socialBackupRepository.writeBackup(
            walletId = walletId,
            createdAtMillis = System.currentTimeMillis(),
            secret = secret,
            recoveryKey = recoveryKey,
        ).bind()
    }

    /** Whether the failure means there is no readable owner to fix, as opposed to one we failed to reach */
    private fun SocialLoginError.namesNoWallet(): Boolean = when (this) {
        is SocialLoginError.BackupNotFound, is SocialLoginError.InvalidBackupFile -> true
        else -> false
    }
}