package com.tangem.domain.cloudbackup.repository

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupAccount
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import kotlinx.coroutines.flow.Flow

/**
 * Storage of encrypted wallet backup files in the user's cloud (Google Drive), plus a locally
 * persisted record of which wallets are backed up.
 *
 * The transport methods only move already-encrypted content — the backup file is encrypted before it
 * reaches this repository. The [isBackedUp] status is persisted locally and does not require network.
 */
interface CloudBackupRepository {

    /**
     * Uploads a backup file. If a backup for [walletId] already exists, it is overwritten,
     * so a wallet always has at most one backup file.
     *
     * @param walletId        id of the backed up wallet
     * @param walletName      wallet name, used for the visible file name and backups list

     * @param content         encrypted backup file content (JSON)
     */
    suspend fun uploadBackup(
        walletId: String,
        walletName: String,
        createdAtMillis: Long,
        content: String,
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

    /** Downloads the encrypted content of the backup file with [fileId] */
    suspend fun downloadBackup(fileId: String): Either<CloudBackupError, String>

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