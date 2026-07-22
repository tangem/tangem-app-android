package com.tangem.domain.cloudbackup.repository

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupAccount
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import kotlinx.coroutines.flow.Flow

/**
 * Storage of encrypted wallet backup files in the user's cloud (Google Drive), plus a locally
 * persisted record of which wallets are backed up.
 *
 * Encryption and decryption of the backup file happen inside this layer: callers pass and receive the
 * plaintext [CloudBackupSecretData] and never handle the cipher, KDF or file format. The [isBackedUp]
 * status is persisted locally and does not require network.
 */
interface CloudBackupRepository {

    /**
     * Encrypts [secret] with [password] and uploads the resulting backup file. If a backup for
     * [walletId] already exists, it is overwritten, so a wallet always has at most one backup file.
     *
     * @param walletId        id of the backed up wallet
     * @param walletName      wallet name, used for the visible file name and backups list

     * @param secret          the wallet secret (mnemonic + optional passphrase) to encrypt
     * @param password        user password the backup is encrypted with
     */
    suspend fun uploadBackup(
        walletId: String,
        walletName: String,
        createdAtMillis: Long,
        secret: CloudBackupSecretData,
        password: CharArray,
    ): Either<CloudBackupError, CloudBackupInfo>

    /**
     * Finds all Tangem backup files in the cloud account. With [interactive] `false` (default) never
     * triggers the account picker, failing with [CloudBackupError.AuthRequired] when not authorized.
     */
    suspend fun findBackups(interactive: Boolean = false): Either<CloudBackupError, List<CloudBackupInfo>>

    /**
     * Returns the currently authorized cloud account (email, name). With [interactive] `false` (default)
     * never triggers the account picker, failing with [CloudBackupError.AuthRequired] when not authorized.
     */
    suspend fun getAccountInfo(interactive: Boolean = false): Either<CloudBackupError, CloudBackupAccount>

    /**
     * Forgets the authorized cloud account, revoking the granted access (best-effort) so the next
     * authorization prompts the account picker again. Used to switch accounts.
     */
    suspend fun signOut()

    /**
     * Downloads the backup file with [fileId] and decrypts it with [password], returning the wallet
     * secret. Fails with [CloudBackupError.WrongPassword] on a bad password and
     * [CloudBackupError.InvalidBackupFile] on a malformed/unsupported file.
     */
    suspend fun readBackup(fileId: String, password: CharArray): Either<CloudBackupError, CloudBackupSecretData>

    /** Deletes the backup file with [fileId]. Deleting an already absent file is a success */
    suspend fun deleteBackup(fileId: String): Either<CloudBackupError, Unit>

    /**
     * Locally persisted record of whether [walletId] has a cloud backup. Survives without network
     * access, so callers can tell a wallet is backed up without listing the cloud storage.
     */
    suspend fun isBackedUp(walletId: String): Boolean

    /** Reactive variant of [isBackedUp] */
    fun isBackedUpFlow(walletId: String): Flow<Boolean>

    /** Records whether [walletId] has a cloud backup */
    suspend fun setBackedUp(walletId: String, backedUp: Boolean)
}