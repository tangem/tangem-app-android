package com.tangem.data.sociallogin.backup

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.sociallogin.SocialBackupJson
import com.tangem.data.sociallogin.crypto.SocialBackupCipher
import com.tangem.data.sociallogin.crypto.SocialBackupCryptoError
import com.tangem.data.sociallogin.crypto.SocialBackupFileData
import com.tangem.data.sociallogin.crypto.SocialBackupPayload
import com.tangem.data.sociallogin.store.SocialBackupStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.RestoredSocialBackup
import com.tangem.domain.sociallogin.models.SocialBackupInfo
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.google.auth.GoogleAuthError
import com.tangem.google.drive.DriveFile
import com.tangem.google.drive.DriveFileMetadata
import com.tangem.google.drive.GoogleDriveApi
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream

/**
 * [SocialBackupRepository] backed by the hidden `appDataFolder` space of the user's Google Drive —
 * invisible in the Drive UI and unreadable by any other app.
 *
 * The folder holds exactly one backup file per cloud account. A write therefore uploads a brand new
 * file first and only then drops every other file in the folder, so a failed write leaves the previous
 * valid backup in place instead of a folder with nothing readable in it.
 */
internal class DefaultSocialBackupRepository(
    private val api: GoogleDriveApi,
    private val tokenProvider: GoogleDriveTokenProvider,
    private val store: SocialBackupStore,
    private val cipher: SocialBackupCipher,
    private val dispatchers: CoroutineDispatcherProvider,
) : SocialBackupRepository {

    override suspend fun writeBackup(
        walletId: UserWalletId,
        createdAtMillis: Long,
        secret: SocialBackupSecret,
        recoveryKey: RecoveryKey,
    ): Either<SocialLoginError, Unit> = withContext(dispatchers.io) {
        val content = withContext(dispatchers.default) {
            encodeBackup(
                secret = secret,
                recoveryKey = recoveryKey,
                walletId = walletId.stringValue,
                createdAtMillis = createdAtMillis,
            )
        } ?: return@withContext Either.Left(SocialLoginError.BackupWriteFailed())

        withAuthRetry { authInteractive ->
            val auth = authHeader(interactive = authInteractive)
            val existingFiles = listBackupFiles(auth)

            val newFileId = createBackupFile(auth)
            val uploaded = either {
                execute(errorHandler = { SocialLoginError.BackupWriteFailed(it) }, notFoundError = null) {
                    api.uploadFileContent(
                        authorization = auth,
                        fileId = newFileId,
                        content = content.toRequestBody(MIME_TYPE_JSON.toMediaType()),
                    )
                }
            }
            uploaded.onLeft { error ->
                // otherwise the half-created file stays behind as a second, unreadable backup
                deleteQuietly(auth, newFileId)
                raise(error)
            }

            existingFiles.forEach { file -> deleteQuietly(auth, file.id) }
        }
    }

    override suspend fun getBackupInfo(): Either<SocialLoginError, SocialBackupInfo> = withContext(dispatchers.io) {
        either {
            val fileData = readBackupFile().bind().data
            SocialBackupInfo(
                walletId = UserWalletId(stringValue = fileData.walletId),
                createdAtMillis = fileData.createdAt.toEpochMillisOrZero(),
                version = fileData.version,
            )
        }
    }

    override suspend fun readBackup(recoveryKey: RecoveryKey): Either<SocialLoginError, RestoredSocialBackup> =
        withContext(dispatchers.io) {
            either {
                val fileData = readBackupFile().bind().data
                val payloadBytes = withContext(dispatchers.default) { cipher.decrypt(fileData, recoveryKey) }
                    .getOrElse { raise(it.toDomainError()) }

                RestoredSocialBackup(
                    info = SocialBackupInfo(
                        walletId = UserWalletId(stringValue = fileData.walletId),
                        createdAtMillis = fileData.createdAt.toEpochMillisOrZero(),
                        version = fileData.version,
                    ),
                    secret = parseSecret(payloadBytes),
                )
            }
        }

    /**
     * Every file owned by [walletId] goes, not just the newest: a write only best-effort cleans up what
     * it replaced, so the folder can hold several files of the same wallet, and leaving an older one
     * behind would keep the wallet restorable after FR-16 reported it unbound. Files of other wallets
     * are left alone.
     */
    override suspend fun deleteBackup(walletId: UserWalletId): Either<SocialLoginError, Unit> =
        withContext(dispatchers.io) {
            withAuthRetry(interactive = false) { authInteractive ->
                val auth = authHeader(interactive = authInteractive)
                val owned = listBackupFiles(auth).filter { file ->
                    val data = readFileData(auth = auth, fileId = file.id)
                    data != null && cipher.validateHeader(data).isRight() && data.walletId == walletId.stringValue
                }

                owned.forEach { file ->
                    val response = catchingIo(onError = { SocialLoginError.BackupWriteFailed(it) }) {
                        api.deleteFile(authorization = auth, fileId = file.id)
                    }
                    // someone else got there first, which is the state this asked for
                    if (!response.isSuccessful && response.code() != HttpURLConnection.HTTP_NOT_FOUND) {
                        raise(
                            response.toError(
                                errorHandler = { SocialLoginError.BackupWriteFailed(it) },
                                notFoundError = null,
                            ),
                        )
                    }
                }
            }
        }

    override suspend fun isSocialBackupCompleted(walletId: UserWalletId): Boolean =
        walletId.stringValue in store.getCompletedWalletIds().first()

    override fun isSocialBackupCompletedFlow(walletId: UserWalletId): Flow<Boolean> =
        store.getCompletedWalletIds().map { walletId.stringValue in it }

    override suspend fun setSocialBackupCompleted(
        walletId: UserWalletId,
        isCompleted: Boolean,
    ): Either<SocialLoginError, Unit> = runSuspendCatching {
        store.setCompleted(walletId = walletId.stringValue, isCompleted = isCompleted)
    }
        .fold(
            onSuccess = { Either.Right(Unit) },
            onFailure = { Either.Left(SocialLoginError.PersistenceFailed(it)) },
        )

    /** `null` when the secret cannot be serialized, i.e. the mnemonic is not a BIP39 phrase */
    private fun encodeBackup(
        secret: SocialBackupSecret,
        recoveryKey: RecoveryKey,
        walletId: String,
        createdAtMillis: Long,
    ): String? {
        val payloadBytes = SocialBackupPayload.encode(
            mnemonic = secret.mnemonic,
            isPassphraseRequired = secret.isPassphraseRequired,
        ) ?: return null

        val fileData = try {
            cipher.encrypt(
                payload = payloadBytes,
                recoveryKey = recoveryKey,
                walletId = walletId,
                createdAt = createdAtMillis.toIsoInstant(),
            )
        } finally {
            payloadBytes.fill(0)
        }
        return SocialBackupJson.encodeToString(fileData)
    }

    /**
     * Unreadable files are skipped rather than treated as the answer: a write that died between
     * creating the file and uploading its content leaves an empty file carrying the newest timestamp,
     * and committing to it would mask the valid backup still sitting underneath.
     */
    private suspend fun readBackupFile(): Either<SocialLoginError, StoredBackup> = withAuthRetry { authInteractive ->
        val auth = authHeader(interactive = authInteractive)
        val files = listBackupFiles(auth).sortedByDescending { it.createdTime.orEmpty() }
        if (files.isEmpty()) raise(SocialLoginError.BackupNotFound)

        // only the header is required of a candidate: a file a newer app wrote still names its owner, and
        // telling whose backup the folder holds — or replacing it — must not depend on being able to
        // decrypt it. Restoring does, and fails on the version inside [readBackup].
        files.firstNotNullOfOrNull { file ->
            val data = readFileData(auth = auth, fileId = file.id) ?: return@firstNotNullOfOrNull null
            if (cipher.validateHeader(data).isLeft()) return@firstNotNullOfOrNull null

            StoredBackup(fileId = file.id, data = data)
        } ?: raise(SocialLoginError.InvalidBackupFile)
    }

    /** `null` when the file holds nothing this app can parse — an empty leftover, or a damaged file */
    private suspend fun Raise<SocialLoginError>.readFileData(auth: String, fileId: String): SocialBackupFileData? {
        val response = catchingIo(onError = { SocialLoginError.Unknown(it) }) {
            api.downloadFileContent(authorization = auth, fileId = fileId)
        }
        // the listing is a snapshot: a file another device dropped meanwhile is one less candidate, not
        // a reason to stop looking at the ones still there
        if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) return null
        if (!response.isSuccessful) {
            raise(response.toError(errorHandler = { SocialLoginError.Unknown(it) }, notFoundError = null))
        }
        val body = response.body() ?: return null
        // .string() streams from the network — read it inside the IO error handling too
        val content = catchingIo(onError = { SocialLoginError.Unknown(it) }) { body.string() }

        return runCatching { SocialBackupJson.decodeFromString<SocialBackupFileData>(content) }.getOrNull()
    }

    private class StoredBackup(val fileId: String, val data: SocialBackupFileData)

    private suspend fun Raise<SocialLoginError>.listBackupFiles(auth: String): List<DriveFile> {
        val response = execute(errorHandler = { SocialLoginError.Unknown(it) }) {
            api.listFiles(
                authorization = auth,
                query = "name='$BACKUP_FILE_NAME' and trashed=false",
                spaces = GoogleDriveApi.SPACE_APP_DATA_FOLDER,
            )
        }
        return response?.files.orEmpty()
    }

    private suspend fun Raise<SocialLoginError>.createBackupFile(auth: String): String {
        val file = execute(errorHandler = { SocialLoginError.BackupWriteFailed(it) }, notFoundError = null) {
            api.createFile(
                authorization = auth,
                metadata = DriveFileMetadata(
                    name = BACKUP_FILE_NAME,
                    parents = listOf(GoogleDriveApi.SPACE_APP_DATA_FOLDER),
                ),
            )
        }
        return ensureNotNull(file?.id) { SocialLoginError.BackupWriteFailed() }
    }

    /**
     * A leftover file that could not be deleted is not worth failing a finished write: restores read
     * the newest file, and the next write tries the cleanup again.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun deleteQuietly(auth: String, fileId: String) {
        try {
            api.deleteFile(authorization = auth, fileId = fileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // leaving the file behind is harmless, see above
        }
    }

    private suspend fun <T> withAuthRetry(
        interactive: Boolean = true,
        block: suspend Raise<SocialLoginError>.(interactive: Boolean) -> T,
    ): Either<SocialLoginError, T> {
        val first = either { block(interactive) }
        val isTokenExpired = first.fold(ifLeft = { it == SocialLoginError.CloudUnavailable }, ifRight = { false })
        // the first 401/403 invalidated the cached token; refresh silently so an expired session
        // recovers without a second account picker — a real re-consent still fails
        return if (isTokenExpired) either { block(false) } else first
    }

    private suspend fun Raise<SocialLoginError>.authHeader(interactive: Boolean): String {
        val token = tokenProvider.getAccessToken(interactive).mapLeft(GoogleAuthError::toDomainError).bind()
        return "Bearer $token"
    }

    private suspend fun <T> Raise<SocialLoginError>.execute(
        errorHandler: (Throwable?) -> SocialLoginError,
        notFoundError: SocialLoginError? = SocialLoginError.BackupNotFound,
        call: suspend () -> Response<T>,
    ): T? {
        val response = catchingIo(onError = errorHandler) { call() }
        if (!response.isSuccessful) raise(response.toError(errorHandler, notFoundError))
        return response.body()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> Raise<SocialLoginError>.catchingIo(
        onError: (Throwable) -> SocialLoginError = { SocialLoginError.Unknown(it) },
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            raise(SocialLoginError.NetworkError)
        } catch (e: Exception) {
            raise(onError(e))
        }
    }

    /**
     * @param notFoundError what a 404 means for this call. `null` for the write path, where a file that
     * went missing is a failed write rather than the answer "there is no backup".
     */
    private suspend fun <T> Response<T>.toError(
        errorHandler: (Throwable?) -> SocialLoginError,
        notFoundError: SocialLoginError?,
    ): SocialLoginError {
        return when (code()) {
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
            -> {
                tokenProvider.invalidate()
                SocialLoginError.CloudUnavailable
            }
            HttpURLConnection.HTTP_NOT_FOUND -> notFoundError ?: errorHandler(null)
            else -> errorHandler(null)
        }
    }

    private fun Long.toIsoInstant(): String = Instant.fromEpochSeconds(TimeUnit.MILLISECONDS.toSeconds(this)).toString()

    /** The timestamp is free-form even in a valid header — it only labels the file */
    private fun String.toEpochMillisOrZero(): Long =
        runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrDefault(0L)

    private companion object {
        const val BACKUP_FILE_NAME = "social-backup.json"
        const val MIME_TYPE_JSON = "application/json"
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun Raise<SocialLoginError>.parseSecret(bytes: ByteArray): SocialBackupSecret {
    // from the stream, not `bytes.decodeToString()`: an intermediate String would hold the mnemonic
    // where it cannot be wiped, unlike the CharArray the parser hands over
    val raw = try {
        runCatching {
            SocialBackupJson.decodeFromStream<SocialBackupPayload>(ByteArrayInputStream(bytes))
        }.getOrNull()
    } finally {
        bytes.fill(0)
    }
    val payload = ensureNotNull(raw) { SocialLoginError.InvalidBackupFile }
    val isPassphraseRequired = payload.isPassphraseRequired
    if (isPassphraseRequired == null) {
        payload.mnemonic.fill(' ')
        raise(SocialLoginError.InvalidBackupFile)
    }
    return SocialBackupSecret(mnemonic = payload.mnemonic, isPassphraseRequired = isPassphraseRequired)
}

private fun SocialBackupCryptoError.toDomainError(): SocialLoginError = when (this) {
    is SocialBackupCryptoError.UnsupportedVersion -> SocialLoginError.UnsupportedBackupVersion(version)
    is SocialBackupCryptoError.InvalidFormat -> SocialLoginError.InvalidBackupFile
}

private fun GoogleAuthError.toDomainError(): SocialLoginError = when (this) {
    // kept out of [SocialLoginError.CloudUnavailable] on purpose: that one is what the silent retry keys
    // on, and asking again for a grant the viewer just declined can only fail a second time
    GoogleAuthError.AuthCanceled -> SocialLoginError.SignInCanceled
    GoogleAuthError.AuthRequired,
    GoogleAuthError.PermissionsMissing,
    GoogleAuthError.Unavailable,
    -> SocialLoginError.CloudUnavailable
    GoogleAuthError.NetworkError -> SocialLoginError.NetworkError
    is GoogleAuthError.Unknown -> SocialLoginError.Unknown(cause)
}