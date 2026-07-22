package com.tangem.data.cloudbackup.repository

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.cloudbackup.CloudBackupJson
import com.tangem.data.cloudbackup.crypto.CloudBackupCipher
import com.tangem.data.cloudbackup.crypto.CloudBackupCryptoError
import com.tangem.data.cloudbackup.crypto.CloudBackupFileData
import com.tangem.data.cloudbackup.crypto.CloudBackupSecret
import com.tangem.data.cloudbackup.datasource.DriveFile
import com.tangem.data.cloudbackup.datasource.DriveFileMetadata
import com.tangem.data.cloudbackup.datasource.GoogleDriveApi
import com.tangem.data.cloudbackup.datasource.GoogleDriveTokenProvider
import com.tangem.data.cloudbackup.store.CloudBackupStore
import com.tangem.domain.cloudbackup.models.CloudBackupAccount
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * [CloudBackupRepository] backed by the Google Drive API v3.
 *
 * Backup files are stored in a visible (not hidden) folder [BACKUPS_FOLDER_NAME] of "My Drive"
 * (FR-03, transparency over security by obscurity). Tangem files are marked and looked up
 * by [KEY_IS_TANGEM_BACKUP] in `appProperties`, so renames done by the user don't break discovery.
 */
internal class DefaultCloudBackupRepository(
    private val api: GoogleDriveApi,
    private val tokenProvider: GoogleDriveTokenProvider,
    private val store: CloudBackupStore,
    private val cipher: CloudBackupCipher,
    private val dispatchers: CoroutineDispatcherProvider,
) : CloudBackupRepository {

    override suspend fun uploadBackup(
        walletId: String,
        walletName: String,
        createdAtMillis: Long,
        secret: CloudBackupSecretData,
        password: CharArray,
    ): Either<CloudBackupError, CloudBackupInfo> = withContext(dispatchers.io) {
        val content = withContext(dispatchers.default) {
            encryptBackup(
                secret = secret,
                walletId = walletId,
                walletName = walletName,
                createdAtMillis = createdAtMillis,
                password = password,
            )
        }
        withAuthRetry { authInteractive ->
            val auth = authHeader(interactive = authInteractive)
            val existingFiles = findBackupFiles(auth)
            val existingFile = existingFiles.firstOrNull { file ->
                file.appProperties?.get(KEY_WALLET_ID) == walletId
            }
            val fileId = if (existingFile != null) {
                val existingNames = existingFiles
                    .filter { it.id != existingFile.id }
                    .mapNotNull(DriveFile::name)
                    .toSet()
                // refresh metadata so the backups list and file name reflect the latest wallet name / timestamp
                execute(errorHandler = { CloudBackupError.WriteError(it) }) {
                    api.updateFileMetadata(
                        authorization = auth,
                        fileId = existingFile.id,
                        metadata = DriveFileMetadata(
                            name = resolveUniqueBackupName(walletName, BACKUP_FILE_EXTENSION, existingNames),
                            appProperties = backupAppProperties(walletId, walletName, createdAtMillis),
                        ),
                    )
                }
                existingFile.id
            } else {
                createBackupFile(
                    auth = auth,
                    walletId = walletId,
                    walletName = walletName,
                    createdAtMillis = createdAtMillis,
                    existingNames = existingFiles.mapNotNull(DriveFile::name).toSet(),
                )
            }

            execute(errorHandler = { CloudBackupError.WriteError(it) }) {
                api.uploadFileContent(
                    authorization = auth,
                    fileId = fileId,
                    content = content.toRequestBody(MIME_TYPE_JSON.toMediaType()),
                )
            }

            CloudBackupInfo(
                fileId = fileId,
                walletName = walletName,
                createdAtMillis = createdAtMillis,
                walletId = walletId,
            )
        }
    }

    override suspend fun findBackups(interactive: Boolean): Either<CloudBackupError, List<CloudBackupInfo>> {
        return withContext(dispatchers.io) {
            withAuthRetry(interactive = interactive) { authInteractive ->
                findBackupFiles(authHeader(interactive = authInteractive)).map { file ->
                    CloudBackupInfo(
                        fileId = file.id,
                        walletName = file.appProperties?.get(KEY_WALLET_NAME) ?: file.name.orEmpty(),
                        createdAtMillis = file.resolveCreatedAtMillis(),
                        walletId = file.appProperties?.get(KEY_WALLET_ID),
                    )
                }
            }
        }
    }

    override suspend fun getAccountInfo(interactive: Boolean): Either<CloudBackupError, CloudBackupAccount> {
        return withContext(dispatchers.io) {
            withAuthRetry(interactive = interactive) { authInteractive ->
                val about = execute(errorHandler = { CloudBackupError.ReadError(it) }) {
                    api.getAbout(authHeader(interactive = authInteractive))
                }
                val user = ensureNotNull(about?.user) { CloudBackupError.ReadError() }
                val email = ensureNotNull(user.emailAddress) { CloudBackupError.ReadError() }
                CloudBackupAccount(
                    email = email,
                    displayName = user.displayName,
                    photoUrl = user.photoLink,
                )
            }
        }
    }

    override suspend fun signOut() = withContext(dispatchers.io) {
        tokenProvider.signOut()
    }

    override suspend fun readBackup(
        fileId: String,
        password: CharArray,
    ): Either<CloudBackupError, CloudBackupSecretData> = withContext(dispatchers.io) {
        either {
            val content = downloadContent(fileId).getOrElse { raise(it) }
            val fileData = ensureNotNull(
                runCatching { CloudBackupJson.decodeFromString<CloudBackupFileData>(content) }.getOrNull(),
            ) { CloudBackupError.InvalidBackupFile }
            val payloadBytes = withContext(dispatchers.default) { cipher.decrypt(fileData, password) }
                .getOrElse { raise(it.toDomainError()) }
            parseSecret(payloadBytes)
        }
    }

    private suspend fun downloadContent(fileId: String): Either<CloudBackupError, String> {
        return withAuthRetry { authInteractive ->
            val body = ensureNotNull(
                execute(errorHandler = { CloudBackupError.ReadError(it) }) {
                    api.downloadFileContent(
                        authorization = authHeader(interactive = authInteractive),
                        fileId = fileId,
                    )
                },
            ) { CloudBackupError.BackupNotFound }
            // .string() streams from the network — read it inside the IO error handling too
            catchingIo(onError = { CloudBackupError.ReadError(it) }) { body.string() }
        }
    }

    private fun encryptBackup(
        secret: CloudBackupSecretData,
        walletId: String,
        walletName: String,
        createdAtMillis: Long,
        password: CharArray,
    ): String {
        val payload = CloudBackupSecret(mnemonic = secret.mnemonic, passphrase = secret.passphrase)
        val payloadBytes = CloudBackupJson.encodeToString(payload)
            .toByteArray(Charsets.UTF_8)
        val createdAtIso = Instant.fromEpochSeconds(TimeUnit.MILLISECONDS.toSeconds(createdAtMillis)).toString()
        val fileData = try {
            cipher.encrypt(
                secret = payloadBytes,
                password = password,
                metadata = CloudBackupCipher.Metadata(
                    name = walletName,
                    walletId = walletId,
                    createdAt = createdAtIso,
                ),
            )
        } finally {
            payloadBytes.fill(0)
        }
        return CloudBackupJson.encodeToString(fileData)
    }

    override suspend fun deleteBackup(fileId: String): Either<CloudBackupError, Unit> {
        return withContext(dispatchers.io) {
            withAuthRetry { authInteractive ->
                val auth = authHeader(interactive = authInteractive)
                val response = catchingIo(onError = { CloudBackupError.WriteError(it) }) {
                    api.deleteFile(authorization = auth, fileId = fileId)
                }

                // deleting an already absent file is a success (backup is desynced, UC-07 alt flow 5.2)
                if (!response.isSuccessful && response.code() != HttpURLConnection.HTTP_NOT_FOUND) {
                    raise(response.toError { CloudBackupError.WriteError(it) })
                }
            }
        }
    }

    override suspend fun isBackedUp(walletId: String): Boolean {
        return walletId in store.getBackedUpWalletIds().first()
    }

    override fun isBackedUpFlow(walletId: String): Flow<Boolean> {
        return store.getBackedUpWalletIds().map { walletId in it }
    }

    override suspend fun setBackedUp(walletId: String, backedUp: Boolean) {
        store.setBackedUp(walletId, backedUp)
    }

    // legacy backups may miss the createdAt appProperty — fall back to the Drive file's createdTime
    private fun DriveFile.resolveCreatedAtMillis(): Long {
        appProperties?.get(KEY_CREATED_AT)?.toLongOrNull()?.let { return it }
        return createdTime?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() } ?: 0L
    }

    private suspend fun <T> withAuthRetry(
        interactive: Boolean = true,
        block: suspend Raise<CloudBackupError>.(interactive: Boolean) -> T,
    ): Either<CloudBackupError, T> {
        val first = either { block(interactive) }
        val isTokenExpired = first.fold(ifLeft = { it == CloudBackupError.AuthPermissionsMissing }, ifRight = { false })
        // the first 401/403 invalidated the cached token; refresh it silently (non-interactive) and retry once,
        // so an expired session recovers without a second account picker — a real re-consent still surfaces the error
        return if (isTokenExpired) either { block(false) } else first
    }

    private suspend fun Raise<CloudBackupError>.authHeader(interactive: Boolean = true): String {
        val token = tokenProvider.getAccessToken(interactive = interactive).bind()
        return "Bearer $token"
    }

    private suspend fun Raise<CloudBackupError>.findBackupFiles(auth: String): List<DriveFile> {
        val response = execute(errorHandler = { CloudBackupError.ReadError(it) }) {
            api.listFiles(
                authorization = auth,
                query = "appProperties has { key='$KEY_IS_TANGEM_BACKUP' and value='true' } and trashed=false",
            )
        }
        return response?.files.orEmpty()
    }

    private suspend fun Raise<CloudBackupError>.createBackupFile(
        auth: String,
        walletId: String,
        walletName: String,
        createdAtMillis: Long,
        existingNames: Set<String>,
    ): String {
        val folderId = ensureBackupsFolder(auth)
        val file = execute(errorHandler = { CloudBackupError.WriteError(it) }) {
            api.createFile(
                authorization = auth,
                metadata = DriveFileMetadata(
                    name = resolveUniqueBackupName(walletName, BACKUP_FILE_EXTENSION, existingNames),
                    parents = listOf(folderId),
                    appProperties = backupAppProperties(walletId, walletName, createdAtMillis),
                ),
            )
        }
        return ensureNotNull(file?.id) { CloudBackupError.WriteError() }
    }

    private fun backupAppProperties(walletId: String, walletName: String, createdAtMillis: Long): Map<String, String> =
        mapOf(
            KEY_IS_TANGEM_BACKUP to "true",
            KEY_WALLET_ID to walletId,
            KEY_WALLET_NAME to walletName,
            KEY_CREATED_AT to createdAtMillis.toString(),
        )

    /** Returns the id of the visible backups folder, creating it if needed */
    private suspend fun Raise<CloudBackupError>.ensureBackupsFolder(auth: String): String {
        val existing = execute(errorHandler = { CloudBackupError.WriteError(it) }) {
            api.listFiles(
                authorization = auth,
                query = "mimeType='$MIME_TYPE_FOLDER' and name='$BACKUPS_FOLDER_NAME' and " +
                    "trashed=false and '$DRIVE_ROOT' in parents",
            )
        }
        existing?.files?.firstOrNull()?.let { return it.id }

        val created = execute(errorHandler = { CloudBackupError.WriteError(it) }) {
            api.createFile(
                authorization = auth,
                metadata = DriveFileMetadata(
                    name = BACKUPS_FOLDER_NAME,
                    mimeType = MIME_TYPE_FOLDER,
                    parents = listOf(DRIVE_ROOT),
                ),
            )
        }
        return ensureNotNull(created?.id) { CloudBackupError.WriteError() }
    }

    private suspend fun <T> Raise<CloudBackupError>.execute(
        errorHandler: (Throwable?) -> CloudBackupError,
        call: suspend () -> Response<T>,
    ): T? {
        val response = catchingIo(onError = { errorHandler(it) }) { call() }
        if (!response.isSuccessful) raise(response.toError(errorHandler))
        return response.body()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> Raise<CloudBackupError>.catchingIo(
        onError: (Throwable) -> CloudBackupError = { CloudBackupError.Unknown(it) },
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            raise(CloudBackupError.NetworkError)
        } catch (e: Exception) {
            raise(onError(e))
        }
    }

    private suspend fun <T> Response<T>.toError(errorHandler: (Throwable?) -> CloudBackupError): CloudBackupError {
        return when (code()) {
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
            -> {
                tokenProvider.invalidate()
                CloudBackupError.AuthPermissionsMissing
            }
            HttpURLConnection.HTTP_NOT_FOUND -> CloudBackupError.BackupNotFound
            else -> errorHandler(null)
        }
    }

    private companion object {
        const val BACKUPS_FOLDER_NAME = "Tangem"
        const val BACKUP_FILE_EXTENSION = "backup.json"
        const val DRIVE_ROOT = "root"

        const val KEY_IS_TANGEM_BACKUP = "tangemBackup"
        const val KEY_WALLET_ID = "walletId"
        const val KEY_WALLET_NAME = "walletName"
        const val KEY_CREATED_AT = "createdAt"

        const val MIME_TYPE_JSON = "application/json"
        const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
    }
}

/**
 * Google Drive permits duplicate file names, so we mimic the OS file-manager behaviour and append an
 * incrementing " (n)" suffix before the extension when "[walletName].[extension]" is already taken.
 * The name is cosmetic (a backup is identified by its `walletId` appProperty), so strict uniqueness
 * across concurrent uploads isn't required.
 */
internal fun resolveUniqueBackupName(walletName: String, extension: String, existingNames: Set<String>): String {
    val base = "$walletName.$extension"
    if (base !in existingNames) return base

    return generateSequence(1) { it + 1 }
        .map { index -> "$walletName ($index).$extension" }
        .first { it !in existingNames }
}

private fun Raise<CloudBackupError>.parseSecret(bytes: ByteArray): CloudBackupSecretData {
    val raw = try {
        runCatching {
            CloudBackupJson.decodeFromString<CloudBackupSecret>(bytes.toString(Charsets.UTF_8))
        }.getOrNull()
    } finally {
        bytes.fill(0)
    }
    val secret = ensureNotNull(raw) { CloudBackupError.InvalidBackupFile }
    return CloudBackupSecretData(mnemonic = secret.mnemonic, passphrase = secret.passphrase)
}

private fun CloudBackupCryptoError.toDomainError(): CloudBackupError = when (this) {
    CloudBackupCryptoError.WrongPassword -> CloudBackupError.WrongPassword
    is CloudBackupCryptoError.InvalidFormat -> CloudBackupError.InvalidBackupFile
}