package com.tangem.data.cloudbackup.repository

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.cloudbackup.CloudBackupJson
import com.tangem.data.cloudbackup.crypto.CloudBackupCipher
import com.tangem.data.cloudbackup.crypto.CloudBackupCryptoError
import com.tangem.data.cloudbackup.crypto.CloudBackupFileData
import com.tangem.data.cloudbackup.datasource.DriveFile
import com.tangem.data.cloudbackup.datasource.DriveFileListResponse
import com.tangem.data.cloudbackup.datasource.DriveFileMetadata
import com.tangem.data.cloudbackup.datasource.GoogleDriveApi
import com.tangem.data.cloudbackup.datasource.GoogleDriveTokenProvider
import com.tangem.data.cloudbackup.store.CloudBackupStore
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultCloudBackupRepositoryTest {

    private val api: GoogleDriveApi = mockk()
    private val tokenProvider: GoogleDriveTokenProvider = mockk(relaxUnitFun = true)
    private val store = FakeCloudBackupStore()

    // Encryption itself is covered by CloudBackupCipherTest; here the cipher is stubbed so transport tests
    // don't pay the Argon2 cost and stay focused on the Drive API orchestration + error mapping.
    private val cipher: CloudBackupCipher = mockk()

    private val repository = DefaultCloudBackupRepository(
        api = api,
        tokenProvider = tokenProvider,
        store = store,
        cipher = cipher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val secret = CloudBackupSecretData(mnemonic = "m", passphrase = null)

    private val fileData = CloudBackupFileData(
        version = 1,
        id = "id",
        crypto = CloudBackupFileData.CryptoData(
            cipher = "aes-256-gcm",
            cipherparams = CloudBackupFileData.CipherParams(nonce = "00"),
            ciphertext = "00",
            tag = "00",
            kdf = "argon2id",
            kdfparams = CloudBackupFileData.KdfParams(
                version = 19,
                memory = 64,
                iterations = 1,
                parallelism = 1,
                dklen = 32,
                salt = "00",
            ),
        ),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(api, tokenProvider, cipher)
        coEvery { tokenProvider.getAccessToken(any()) } returns "token".right()
        every { cipher.encrypt(any(), any(), any(), any()) } returns fileData
    }

    @Test
    fun `GIVEN 401 then 200 WHEN readBackup THEN token invalidated AND request retried once`() = runTest {
        // Arrange
        coEvery { api.downloadFileContent(any(), any(), any()) } returnsMany listOf(
            errorResponse(HTTP_UNAUTHORIZED),
            successResponse(CloudBackupJson.encodeToString(fileData)),
        )
        every { cipher.decrypt(any(), any()) } returns SECRET_JSON.toByteArray(Charsets.UTF_8).right()

        // Act
        val actual = repository.readBackup(fileId = "file-1", password = "p".toCharArray())

        // Assert
        assertThat(actual).isEqualTo(CloudBackupSecretData(mnemonic = "m", passphrase = null).right())
        coVerify(exactly = 1) { tokenProvider.invalidate() }
        coVerify(exactly = 2) { api.downloadFileContent(any(), any(), any()) }
    }

    @Test
    fun `GIVEN persistent 401 WHEN readBackup THEN retried once then AuthPermissionsMissing`() = runTest {
        // Arrange
        coEvery { api.downloadFileContent(any(), any(), any()) } returns errorResponse(HTTP_UNAUTHORIZED)

        // Act
        val actual = repository.readBackup(fileId = "file-1", password = "p".toCharArray())

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.AuthPermissionsMissing.left())
        coVerify(exactly = 2) { api.downloadFileContent(any(), any(), any()) }
    }

    @Test
    fun `GIVEN 401 then 200 WHEN readBackup THEN retry requests token non-interactively`() = runTest {
        // Arrange
        val requestedInteractive = mutableListOf<Boolean>()
        coEvery { tokenProvider.getAccessToken(capture(requestedInteractive)) } returns "token".right()
        coEvery { api.downloadFileContent(any(), any(), any()) } returnsMany listOf(
            errorResponse(HTTP_UNAUTHORIZED),
            successResponse(CloudBackupJson.encodeToString(fileData)),
        )
        every { cipher.decrypt(any(), any()) } returns SECRET_JSON.toByteArray(Charsets.UTF_8).right()

        // Act
        repository.readBackup(fileId = "file-1", password = "p".toCharArray())

        // Assert
        assertThat(requestedInteractive).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN wrong password WHEN readBackup THEN WrongPassword`() = runTest {
        // Arrange
        coEvery { api.downloadFileContent(any(), any(), any()) } returns successResponse(CloudBackupJson.encodeToString(fileData))
        every { cipher.decrypt(any(), any()) } returns CloudBackupCryptoError.WrongPassword.left()

        // Act
        val actual = repository.readBackup(fileId = "file-1", password = "p".toCharArray())

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.WrongPassword.left())
    }

    @Test
    fun `GIVEN malformed content WHEN readBackup THEN InvalidBackupFile`() = runTest {
        // Arrange
        coEvery { api.downloadFileContent(any(), any(), any()) } returns successResponse("not a backup file")

        // Act
        val actual = repository.readBackup(fileId = "file-1", password = "p".toCharArray())

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.InvalidBackupFile.left())
    }

    @Test
    fun `GIVEN nothing stored WHEN isBackedUp THEN returns false`() = runTest {
        assertThat(repository.isBackedUp(WALLET_ID)).isFalse()
    }

    @Test
    fun `GIVEN setBackedUp true WHEN isBackedUp THEN returns true`() = runTest {
        // Act
        repository.setBackedUp(WALLET_ID, backedUp = true)

        // Assert
        assertThat(repository.isBackedUp(WALLET_ID)).isTrue()
    }

    @Test
    fun `GIVEN backed up WHEN setBackedUp false THEN isBackedUp returns false`() = runTest {
        // Arrange
        repository.setBackedUp(WALLET_ID, backedUp = true)

        // Act
        repository.setBackedUp(WALLET_ID, backedUp = false)

        // Assert
        assertThat(repository.isBackedUp(WALLET_ID)).isFalse()
    }

    @Test
    fun `GIVEN nothing stored WHEN isBackedUpFlow THEN emits false`() = runTest {
        assertThat(repository.isBackedUpFlow(WALLET_ID).first()).isFalse()
    }

    @Test
    fun `GIVEN setBackedUp true WHEN isBackedUpFlow THEN emits true`() = runTest {
        // Arrange
        repository.setBackedUp(WALLET_ID, backedUp = true)

        // Assert
        assertThat(repository.isBackedUpFlow(WALLET_ID).first()).isTrue()
    }

    @Test
    fun `GIVEN isBackedUpFlow collected WHEN backup state toggles THEN emits each change`() = runTest {
        repository.isBackedUpFlow(WALLET_ID).test {
            // initial state — nothing stored
            assertThat(awaitItem()).isFalse()

            repository.setBackedUp(WALLET_ID, backedUp = true)
            assertThat(awaitItem()).isTrue()

            repository.setBackedUp(WALLET_ID, backedUp = false)
            assertThat(awaitItem()).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN same-named backup exists WHEN uploadBackup for a new wallet THEN file name is incremented`() = runTest {
        // Arrange
        coEvery { api.listFiles(any(), any(), any()) } answers {
            val query = secondArg<String>()
            val files = if (query.contains("mimeType")) {
                listOf(DriveFile(id = "folder-id", name = "Tangem"))
            } else {
                listOf(
                    DriveFile(
                        id = "existing-id",
                        name = "Wallet.backup.json",
                        appProperties = mapOf("walletId" to "other-wallet"),
                    ),
                )
            }
            Response.success(DriveFileListResponse(files = files))
        }
        val metadata = slot<DriveFileMetadata>()
        coEvery { api.createFile(any(), capture(metadata), any()) } returns Response.success(DriveFile(id = "new-id"))
        coEvery {
            api.uploadFileContent(any(), any(), any(), any(), any())
        } returns Response.success(DriveFile(id = "new-id"))

        // Act
        repository.uploadBackup(
            walletId = "new-wallet",
            walletName = "Wallet",
            createdAtMillis = 0L,
            secret = secret,
            password = "p".toCharArray(),
        )

        // Assert
        assertThat(metadata.captured.name).isEqualTo("Wallet (1).backup.json")
    }

    @Test
    fun `GIVEN backup exists for wallet WHEN uploadBackup THEN metadata is refreshed instead of created`() = runTest {
        // Arrange
        coEvery { api.listFiles(any(), any(), any()) } returns Response.success(
            DriveFileListResponse(
                files = listOf(
                    DriveFile(
                        id = "existing-id",
                        name = "OldName.backup.json",
                        appProperties = mapOf(
                            "walletId" to "wallet-1",
                            "walletName" to "OldName",
                            "createdAt" to "1000",
                        ),
                    ),
                ),
            ),
        )
        val metadata = slot<DriveFileMetadata>()
        coEvery {
            api.updateFileMetadata(any(), "existing-id", capture(metadata), any())
        } returns Response.success(DriveFile(id = "existing-id"))
        coEvery {
            api.uploadFileContent(any(), any(), any(), any(), any())
        } returns Response.success(DriveFile(id = "existing-id"))

        // Act
        repository.uploadBackup(
            walletId = "wallet-1",
            walletName = "NewName",
            createdAtMillis = 2000L,
            secret = secret,
            password = "p".toCharArray(),
        )

        // Assert
        assertThat(metadata.captured.name).isEqualTo("NewName.backup.json")
        assertThat(metadata.captured.appProperties).containsEntry("walletName", "NewName")
        assertThat(metadata.captured.appProperties).containsEntry("createdAt", "2000")
        coVerify(exactly = 0) { api.createFile(any(), any(), any()) }
    }

    @Test
    fun `GIVEN new backup WHEN uploadBackup THEN backups folder is looked up in Drive root`() = runTest {
        // Arrange
        val queries = mutableListOf<String>()
        coEvery { api.listFiles(any(), capture(queries), any()) } answers {
            val query = secondArg<String>()
            val files = if (query.contains("mimeType")) listOf(DriveFile(id = "folder-id", name = "Tangem")) else emptyList()
            Response.success(DriveFileListResponse(files = files))
        }
        coEvery { api.createFile(any(), any(), any()) } returns Response.success(DriveFile(id = "new-id"))
        coEvery {
            api.uploadFileContent(any(), any(), any(), any(), any())
        } returns Response.success(DriveFile(id = "new-id"))

        // Act
        repository.uploadBackup(
            walletId = "w1",
            walletName = "Wallet",
            createdAtMillis = 0L,
            secret = secret,
            password = "p".toCharArray(),
        )

        // Assert
        assertThat(queries.any { it.contains("'root' in parents") }).isTrue()
    }

    @Test
    fun `GIVEN backups folder absent WHEN uploadBackup THEN folder is created in Drive root`() = runTest {
        // Arrange
        coEvery { api.listFiles(any(), any(), any()) } returns Response.success(DriveFileListResponse(files = emptyList()))
        val created = mutableListOf<DriveFileMetadata>()
        coEvery { api.createFile(any(), capture(created), any()) } returns Response.success(DriveFile(id = "id"))
        coEvery {
            api.uploadFileContent(any(), any(), any(), any(), any())
        } returns Response.success(DriveFile(id = "id"))

        // Act
        repository.uploadBackup(
            walletId = "w1",
            walletName = "Wallet",
            createdAtMillis = 0L,
            secret = secret,
            password = "p".toCharArray(),
        )

        // Assert
        val folderMetadata = created.single { it.mimeType == FOLDER_MIME_TYPE }
        assertThat(folderMetadata.parents).containsExactly("root")
    }

    @Test
    fun `GIVEN no files WHEN resolveUniqueBackupName THEN base name`() {
        assertThat(resolveUniqueBackupName("Wallet", "backup.json", emptySet())).isEqualTo("Wallet.backup.json")
    }

    @Test
    fun `GIVEN base name taken WHEN resolveUniqueBackupName THEN first increment`() {
        assertThat(resolveUniqueBackupName("Wallet", "backup.json", setOf("Wallet.backup.json")))
            .isEqualTo("Wallet (1).backup.json")
    }

    @Test
    fun `GIVEN base and first increment taken WHEN resolveUniqueBackupName THEN second increment`() {
        val existing = setOf("Wallet.backup.json", "Wallet (1).backup.json")
        assertThat(resolveUniqueBackupName("Wallet", "backup.json", existing)).isEqualTo("Wallet (2).backup.json")
    }

    @Test
    fun `GIVEN only increment taken but base free WHEN resolveUniqueBackupName THEN base name`() {
        assertThat(resolveUniqueBackupName("Wallet", "backup.json", setOf("Wallet (1).backup.json")))
            .isEqualTo("Wallet.backup.json")
    }

    @Test
    fun `GIVEN a different name taken WHEN resolveUniqueBackupName THEN unaffected`() {
        assertThat(resolveUniqueBackupName("Other", "backup.json", setOf("Wallet.backup.json")))
            .isEqualTo("Other.backup.json")
    }

    @Test
    fun `GIVEN backup without createdAt appProperty WHEN findBackups THEN createdAtMillis from createdTime`() = runTest {
        // Arrange
        val createdTime = "2024-01-15T10:30:00Z"
        coEvery { api.listFiles(any(), any(), any()) } returns Response.success(
            DriveFileListResponse(
                files = listOf(
                    DriveFile(
                        id = "f1",
                        name = "Wallet.backup.json",
                        createdTime = createdTime,
                        appProperties = mapOf("walletId" to "w1"),
                    ),
                ),
            ),
        )

        // Act
        val result = repository.findBackups(interactive = false)

        // Assert
        assertThat(result.getOrNull()?.single()?.createdAtMillis)
            .isEqualTo(Instant.parse(createdTime).toEpochMilliseconds())
    }

    @Test
    fun `GIVEN list throws non-IO exception WHEN findBackups THEN ReadError not Unknown`() = runTest {
        // Arrange
        coEvery { api.listFiles(any(), any(), any()) } throws IllegalStateException("conversion error")

        // Act
        val result = repository.findBackups(interactive = false)

        // Assert
        assertThat(result.isLeft()).isTrue()
        result.onLeft { assertThat(it).isInstanceOf(CloudBackupError.ReadError::class.java) }
    }

    @Test
    fun `GIVEN delete throws non-IO exception WHEN deleteBackup THEN WriteError not Unknown`() = runTest {
        // Arrange
        coEvery { api.deleteFile(any(), any()) } throws IllegalStateException("unexpected error")

        // Act
        val result = repository.deleteBackup(fileId = "f1")

        // Assert
        assertThat(result.isLeft()).isTrue()
        result.onLeft { assertThat(it).isInstanceOf(CloudBackupError.WriteError::class.java) }
    }

    private fun errorResponse(code: Int): Response<ResponseBody> = Response.error(code, "".toResponseBody(null))

    private fun successResponse(content: String): Response<ResponseBody> =
        Response.success(content.toResponseBody(JSON_MEDIA_TYPE))

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val WALLET_ID = "wallet-1"
        const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        const val SECRET_JSON = "{\"mnemonic\":\"m\"}"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

private class FakeCloudBackupStore : CloudBackupStore {

    private val walletIds = MutableStateFlow<Set<String>>(emptySet())

    override fun getBackedUpWalletIds(): Flow<Set<String>> = walletIds

    override suspend fun setBackedUp(walletId: String, backedUp: Boolean) {
        walletIds.update { if (backedUp) it + walletId else it - walletId }
    }
}