package com.tangem.domain.sociallogin.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.RestoredSocialBackup
import com.tangem.domain.sociallogin.models.SocialBackupInfo
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import kotlinx.coroutines.flow.Flow

/**
 * The encrypted `social-backup.json` file in the hidden folder of the user's cloud account, plus the
 * locally persisted record of which wallets have social recovery set up.
 *
 * Encryption and decryption happen inside this layer: callers pass and receive the plaintext
 * [SocialBackupSecret] and never handle the cipher or the file format. The completion flag is stored
 * locally and needs no network, so backup checks (FR-15) never wait on the cloud.
 */
interface SocialBackupRepository {

    /**
     * Encrypts [secret] with [recoveryKey] and writes the backup of [walletId], replacing whatever file
     * the folder held — the folder keeps a single backup regardless of which wallet it belonged to
     * (section 4.3). Each write uses a fresh nonce (CPR-02), and the previous file is only dropped once
     * the new one is stored (NFR-01).
     *
     * The completion flag of the wallet the backup is taken away from is not touched here — that is
     * handled one level up, in [com.tangem.domain.sociallogin.usecase.WriteSocialBackupUseCase].
     */
    suspend fun writeBackup(
        walletId: UserWalletId,
        createdAtMillis: Long,
        secret: SocialBackupSecret,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit>

    /**
     * Plaintext header of the file the folder currently holds, without decrypting it — the only way to
     * tell whose backup is there before a recovery key is available. Fails with
     * [SocialLoginError.BackupNotFound] when the folder is empty.
     *
     * Does not judge the format version: a header of a version this app cannot decrypt still names the
     * wallet the file belongs to, which is exactly what callers deciding whether to replace it need.
     * Whether the file is usable is settled by [readBackup], where the key and the AAD are (CPR-05).
     */
    suspend fun getBackupInfo(): Either<SocialLoginError, SocialBackupInfo>

    /**
     * Reads the backup file from the folder and decrypts it with [recoveryKey].
     *
     * The wallet id is taken from the file rather than from the caller: a device restoring a wallet has
     * no id to check against yet, and the file cannot lie about it — the AAD is built from the file's own
     * version and wallet id, so any change to them fails the AES-GCM authentication (CPR-03). A wrong
     * key, a tampered file and a malformed one are all [SocialLoginError.InvalidBackupFile]; a version
     * this app cannot decrypt is rejected before decryption as
     * [SocialLoginError.UnsupportedBackupVersion] (CPR-05).
     */
    suspend fun readBackup(recoveryKey: RecoveryKey): Either<SocialLoginError, RestoredSocialBackup>

    /**
     * Deletes the backup file if it belongs to [walletId]. A file belonging to another wallet is left
     * alone, and an already absent file is a success — in both cases [walletId] ends up without a backup,
     * which is what the caller asked for.
     */
    suspend fun deleteBackup(walletId: UserWalletId): Either<SocialLoginError, Unit>

    /**
     * Locally persisted record of whether social recovery is complete for [walletId] — set only once
     * both the cloud file and the backend binding are in place (NFR-01). Counts as a backup everywhere a
     * wallet is checked for one (FR-15), so an unreadable state reads as `false` rather than failing:
     * claiming a backup that may not exist is the dangerous direction.
     */
    suspend fun isSocialBackupCompleted(walletId: UserWalletId): Boolean

    /** Reactive variant of [isSocialBackupCompleted] */
    fun isSocialBackupCompletedFlow(walletId: UserWalletId): Flow<Boolean>

    /**
     * Records whether social recovery is complete for [walletId]. Fails with
     * [SocialLoginError.PersistenceFailed] when the state could not be stored, so a flow that was
     * recording progress does not report a success the app cannot back up.
     */
    suspend fun setSocialBackupCompleted(walletId: UserWalletId, isCompleted: Boolean): Either<SocialLoginError, Unit>
}