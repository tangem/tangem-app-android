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
 * Encrypts a wallet secret with the recovery key and writes it to the hidden cloud folder.
 *
 * **Precondition: the binding of the signed-in identity must already be resolved** — either absent, or
 * pointing at [walletId]. Writing is destructive and irreversible while registering the key can still be
 * refused (FR-14), so a write started before the binding is known can destroy a backup that no longer
 * has a replacement. Scenario 1 satisfies this by construction: the folder and the identity belong to
 * the same cloud account, and the create path is only entered after the recover call answered "no
 * binding", which makes any file left in the folder an orphan.
 *
 * The folder holds one backup per cloud account, so writing here may still take the backup away from
 * another wallet (section 4.3). That wallet stops being recoverable the moment its file is replaced, so
 * its completion flag is cleared first — a wallet that still counted as backed up would hide the prompts
 * that FR-15 relies on. Clearing before the write also keeps the failure safe: an aborted write leaves
 * the old wallet marked as not backed up while its file is still there, which only over-prompts.
 *
 * The flag being cleared is the one on THIS device, so the case that matters is a wallet torn down
 * elsewhere: the device that unbound it could not delete the file, and this device still carries its
 * flag. A teardown done here leaves nothing to clear — the flag is dropped before the file is.
 *
 * A header that is absent or cannot be parsed is no obstacle — the file gets replaced regardless of what
 * it holds, and such a header names no wallet whose flag we could fix. A header that failed to arrive is
 * different: the file it describes may well be another wallet's, so the write does not start at all
 * rather than orphan a wallet that keeps claiming to be backed up.
 *
 * Writing the file is only half of the setup — social recovery counts as complete once the key is also
 * registered on the backend (NFR-01), so this does not mark the written wallet as backed up.
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