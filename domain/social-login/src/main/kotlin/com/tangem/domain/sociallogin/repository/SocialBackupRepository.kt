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
 * [SocialBackupSecret]. The completion flag is local, so backup checks never wait on the cloud.
 */
interface SocialBackupRepository {

    /**
     * Replaces whatever file the folder held — it keeps a single backup regardless of which wallet it
     * belonged to. The previous file is dropped only once the new one is stored.
     */
    suspend fun writeBackup(
        walletId: UserWalletId,
        createdAtMillis: Long,
        secret: SocialBackupSecret,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit>

    /** Plaintext header of the stored file — the only way to tell whose backup it is without the key */
    suspend fun getBackupInfo(): Either<SocialLoginError, SocialBackupInfo>

    /**
     * The wallet id comes from the file rather than the caller: a device restoring a wallet has none to
     * check against yet, and the file cannot lie — the AAD is built from its own header. A wrong key, a
     * tampered file and a malformed one are all [SocialLoginError.InvalidBackupFile].
     */
    suspend fun readBackup(recoveryKey: RecoveryKey): Either<SocialLoginError, RestoredSocialBackup>

    /**
     * A file belonging to another wallet is left alone and an already absent file is a success — either
     * way [walletId] ends up without a backup, which is what the caller asked for.
     *
     * Never shows an account picker — it runs as a side effect of the upgrade to a card, not on a user
     * request. Without a grant that can be refreshed silently it fails with
     * [SocialLoginError.CloudUnavailable].
     */
    suspend fun deleteBackup(walletId: UserWalletId): Either<SocialLoginError, Unit>

    /**
     * Set only once both the cloud file and the backend binding are in place. An unreadable state reads
     * as `false`: claiming a backup that may not exist is the dangerous direction.
     */
    suspend fun isSocialBackupCompleted(walletId: UserWalletId): Boolean

    fun isSocialBackupCompletedFlow(walletId: UserWalletId): Flow<Boolean>

    /**
     * Fails with [SocialLoginError.PersistenceFailed] when the state could not be stored, so a flow that
     * was recording progress does not report a success the app cannot back up.
     */
    suspend fun setSocialBackupCompleted(walletId: UserWalletId, isCompleted: Boolean): Either<SocialLoginError, Unit>
}