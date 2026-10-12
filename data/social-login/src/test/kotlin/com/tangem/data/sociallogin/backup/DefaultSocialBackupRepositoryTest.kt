package com.tangem.data.sociallogin.backup

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.data.sociallogin.SocialBackupJson
import com.tangem.data.sociallogin.crypto.SocialBackupCipher
import com.tangem.data.sociallogin.crypto.SocialBackupFileData
import com.tangem.data.sociallogin.crypto.SocialBackupPayload
import com.tangem.data.sociallogin.store.SocialBackupStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialBackupSecret
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.google.auth.GoogleAuthError
import com.tangem.google.drive.DriveFile
import com.tangem.google.drive.DriveFileListResponse
import com.tangem.google.drive.DriveFileMetadata
import com.tangem.google.drive.GoogleDriveApi
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import retrofit2.Response
import java.security.SecureRandom

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialBackupRepositoryTest {

    private val api: GoogleDriveApi = mockk()
    private val tokenProvider: GoogleDriveTokenProvider = mockk(relaxUnitFun = true)
    private val store: SocialBackupStore = mockk(relaxUnitFun = true)
    private val cipher = SocialBackupCipher()

    private val repository = DefaultSocialBackupRepository(
        api = api,
        tokenProvider = tokenProvider,
        store = store,
        cipher = cipher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val recoveryKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES).also(SecureRandom()::nextBytes))

    @BeforeEach
    fun setup() {
        clearMocks(api, tokenProvider, store)
        coEvery { tokenProvider.getAccessToken(any()) } returns Either.Right(ACCESS_TOKEN)
        coEvery { api.deleteFile(any(), any()) } returns Response.success(Unit)
    }

    @Test
    fun `GIVEN a folder with an old backup WHEN writeBackup THEN the new file is uploaded before the old one goes`() =
        runTest {
            // Arrange
            stubList(driveFile(id = OLD_FILE_ID))
            coEvery { api.createFile(any(), any(), any()) } returns Response.success(DriveFile(id = NEW_FILE_ID))
            val uploaded = slot<RequestBody>()
            coEvery {
                api.uploadFileContent(any(), NEW_FILE_ID, capture(uploaded), any(), any())
            } returns Response.success(DriveFile(id = NEW_FILE_ID))

            // Act
            val result = repository.writeBackup(
                walletId = UserWalletId(stringValue = WALLET_ID),
                createdAtMillis = CREATED_AT_MILLIS,
                secret = secret(),
                recoveryKey = recoveryKey,
            )

            // Assert
            assertThat(result.isRight()).isTrue()
            val stored = SocialBackupJson.decodeFromString<SocialBackupFileData>(uploaded.captured.asString())
            assertThat(stored.walletId).isEqualTo(WALLET_ID)
            assertThat(cipher.decrypt(stored, recoveryKey).getOrNull()?.decodeToString())
                .isEqualTo("""{"mnemonic":"$MNEMONIC","passphraseRequired":0}""")
            coVerify(exactly = 1) { api.deleteFile(any(), OLD_FILE_ID) }
        }

    @Test
    fun `GIVEN a wallet WHEN writeBackup THEN the file is listed and created in the hidden folder`() = runTest {
        // Arrange — the hidden folder is the whole point of the change, and Drive defaults to the
        // visible space, so both the listing and the creation are pinned to it
        stubList()
        val spaces = slot<String>()
        val metadata = slot<DriveFileMetadata>()
        coEvery { api.listFiles(any(), any(), any(), capture(spaces)) } returns Response.success(
            DriveFileListResponse(files = emptyList()),
        )
        coEvery { api.createFile(any(), capture(metadata), any()) } returns Response.success(
            DriveFile(id = NEW_FILE_ID),
        )
        coEvery {
            api.uploadFileContent(any(), NEW_FILE_ID, any(), any(), any())
        } returns Response.success(DriveFile(id = NEW_FILE_ID))

        // Act
        val result = repository.writeBackup(
            walletId = UserWalletId(stringValue = WALLET_ID),
            createdAtMillis = CREATED_AT_MILLIS,
            secret = secret(),
            recoveryKey = recoveryKey,
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(spaces.captured).isEqualTo(GoogleDriveApi.SPACE_APP_DATA_FOLDER)
        assertThat(metadata.captured.parents).containsExactly(GoogleDriveApi.SPACE_APP_DATA_FOLDER)
    }

    @Test
    fun `GIVEN the upload fails WHEN writeBackup THEN the half-created file goes and the old one stays`() = runTest {
        // Arrange
        stubList(driveFile(id = OLD_FILE_ID))
        coEvery { api.createFile(any(), any(), any()) } returns Response.success(DriveFile(id = NEW_FILE_ID))
        coEvery { api.uploadFileContent(any(), any(), any(), any(), any()) } returns errorResponse(code = 500)

        // Act
        val result = repository.writeBackup(
            walletId = UserWalletId(stringValue = WALLET_ID),
            createdAtMillis = CREATED_AT_MILLIS,
            secret = secret(),
            recoveryKey = recoveryKey,
        )

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(SocialLoginError.BackupWriteFailed::class.java)
        coVerify(exactly = 1) { api.deleteFile(any(), NEW_FILE_ID) }
        coVerify(exactly = 0) { api.deleteFile(any(), OLD_FILE_ID) }
    }

    @Test
    fun `GIVEN an empty leftover next to a valid backup WHEN getBackupInfo THEN the readable file answers`() =
        runTest {
            // Arrange — the empty file is the newest, which is exactly the state an aborted write leaves
            stubList(
                driveFile(id = NEW_FILE_ID, createdTime = "2026-08-11T00:00:00Z"),
                driveFile(id = OLD_FILE_ID, createdTime = "2026-08-10T00:00:00Z"),
            )
            stubDownload(fileId = NEW_FILE_ID, content = "")
            stubDownload(fileId = OLD_FILE_ID, content = backupJson())

            // Act
            val result = repository.getBackupInfo()

            // Assert
            assertThat(result.getOrNull()?.walletId).isEqualTo(UserWalletId(stringValue = WALLET_ID))
        }

    @Test
    fun `GIVEN a newer file with a malformed header WHEN getBackupInfo THEN the older backup still answers`() =
        runTest {
            // Arrange
            stubList(
                driveFile(id = NEW_FILE_ID, createdTime = "2026-08-11T00:00:00Z"),
                driveFile(id = OLD_FILE_ID, createdTime = "2026-08-10T00:00:00Z"),
            )
            stubDownload(fileId = NEW_FILE_ID, content = backupJson(walletId = "not-hex"))
            stubDownload(fileId = OLD_FILE_ID, content = backupJson())

            // Act
            val result = repository.getBackupInfo()

            // Assert
            assertThat(result.getOrNull()?.walletId).isEqualTo(UserWalletId(stringValue = WALLET_ID))
        }

    @Test
    fun `GIVEN a listed file was dropped meanwhile WHEN getBackupInfo THEN the older backup still answers`() =
        runTest {
            // Arrange
            stubList(
                driveFile(id = NEW_FILE_ID, createdTime = "2026-08-11T00:00:00Z"),
                driveFile(id = OLD_FILE_ID, createdTime = "2026-08-10T00:00:00Z"),
            )
            coEvery { api.downloadFileContent(any(), NEW_FILE_ID, any()) } returns errorResponse(code = 404)
            stubDownload(fileId = OLD_FILE_ID, content = backupJson())

            // Act
            val result = repository.getBackupInfo()

            // Assert
            assertThat(result.getOrNull()?.walletId).isEqualTo(UserWalletId(stringValue = WALLET_ID))
        }

    @Test
    fun `GIVEN a backup written by a newer app WHEN getBackupInfo THEN its owner is still reported`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson(version = UNSUPPORTED_VERSION))

        // Act — the header must stay readable, otherwise the wallet it belongs to can never be replaced
        val result = repository.getBackupInfo()

        // Assert
        assertThat(result.getOrNull()?.walletId).isEqualTo(UserWalletId(stringValue = WALLET_ID))
        assertThat(result.getOrNull()?.version).isEqualTo(UNSUPPORTED_VERSION)
    }

    @Test
    fun `GIVEN a backup written by a newer app WHEN readBackup THEN the version is rejected`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson(version = UNSUPPORTED_VERSION))

        // Act
        val result = repository.readBackup(recoveryKey)

        // Assert
        assertThat(result.leftOrNull())
            .isEqualTo(SocialLoginError.UnsupportedBackupVersion(version = UNSUPPORTED_VERSION))
    }

    @Test
    fun `GIVEN a backup of this wallet WHEN readBackup THEN the secret is recovered`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson())

        // Act
        val result = repository.readBackup(recoveryKey)

        // Assert
        val restored = result.getOrNull()
        assertThat(restored?.secret?.mnemonic).isEqualTo(MNEMONIC.toCharArray())
        assertThat(restored?.secret?.isPassphraseRequired).isFalse()
        assertThat(restored?.info?.walletId).isEqualTo(UserWalletId(stringValue = WALLET_ID))
    }

    @Test
    fun `GIVEN the folder holds another wallet's backup WHEN deleteBackup THEN nothing is deleted`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson(walletId = OTHER_WALLET_ID))

        // Act
        val result = repository.deleteBackup(UserWalletId(stringValue = WALLET_ID))

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { api.deleteFile(any(), any()) }
    }

    @Test
    fun `GIVEN a leftover of the same wallet WHEN deleteBackup THEN every owned file is removed`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID), driveFile(id = OLD_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson())
        stubDownload(fileId = OLD_FILE_ID, content = backupJson())

        // Act
        val result = repository.deleteBackup(UserWalletId(stringValue = WALLET_ID))

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { api.deleteFile(any(), NEW_FILE_ID) }
        coVerify(exactly = 1) { api.deleteFile(any(), OLD_FILE_ID) }
    }

    @Test
    fun `GIVEN another wallet's file alongside WHEN deleteBackup THEN only the owned one is removed`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID), driveFile(id = OLD_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson(walletId = OTHER_WALLET_ID))
        stubDownload(fileId = OLD_FILE_ID, content = backupJson())

        // Act
        val result = repository.deleteBackup(UserWalletId(stringValue = WALLET_ID))

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { api.deleteFile(any(), OLD_FILE_ID) }
        coVerify(exactly = 0) { api.deleteFile(any(), NEW_FILE_ID) }
    }

    @Test
    fun `GIVEN a backup written by a newer app WHEN deleteBackup THEN it is still removed`() = runTest {
        // Arrange
        stubList(driveFile(id = NEW_FILE_ID))
        stubDownload(fileId = NEW_FILE_ID, content = backupJson(version = UNSUPPORTED_VERSION))

        // Act
        val result = repository.deleteBackup(UserWalletId(stringValue = WALLET_ID))

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { api.deleteFile(any(), NEW_FILE_ID) }
    }

    @Test
    fun `GIVEN no cached grant WHEN deleteBackup THEN the cloud is reported unavailable without a picker`() =
        runTest {
            // Arrange
            coEvery { tokenProvider.getAccessToken(any()) } returns Either.Left(GoogleAuthError.AuthRequired)

            // Act
            val result = repository.deleteBackup(UserWalletId(stringValue = WALLET_ID))

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(SocialLoginError.CloudUnavailable)
            coVerify(exactly = 0) { tokenProvider.getAccessToken(interactive = true) }
        }

    @Test
    fun `GIVEN the token expired WHEN getBackupInfo THEN it is invalidated and the call is retried silently`() =
        runTest {
            // Arrange
            coEvery { api.listFiles(any(), any(), any(), any()) } returnsMany listOf(
                errorResponse(code = 401),
                Response.success(DriveFileListResponse(files = listOf(driveFile(id = NEW_FILE_ID)))),
            )
            stubDownload(fileId = NEW_FILE_ID, content = backupJson())

            // Act
            val result = repository.getBackupInfo()

            // Assert
            assertThat(result.getOrNull()?.walletId).isEqualTo(UserWalletId(stringValue = WALLET_ID))
            coVerify(exactly = 1) { tokenProvider.invalidate() }
            coVerify(exactly = 1) { tokenProvider.getAccessToken(interactive = false) }
        }

    @Test
    fun `GIVEN the account was never authorized WHEN getBackupInfo THEN the cloud is reported unavailable`() =
        runTest {
            // Arrange
            coEvery { tokenProvider.getAccessToken(any()) } returns Either.Left(GoogleAuthError.AuthRequired)

            // Act
            val result = repository.getBackupInfo()

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(SocialLoginError.CloudUnavailable)
        }

    @Test
    fun `GIVEN the viewer declined the grant WHEN getBackupInfo THEN it is not asked again`() = runTest {
        // Arrange
        coEvery { tokenProvider.getAccessToken(any()) } returns Either.Left(GoogleAuthError.AuthCanceled)

        // Act
        val result = repository.getBackupInfo()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SocialLoginError.SignInCanceled)
        coVerify(exactly = 1) { tokenProvider.getAccessToken(any()) }
    }

    @Test
    fun `GIVEN an empty folder WHEN getBackupInfo THEN BackupNotFound is returned`() = runTest {
        // Arrange
        stubList()

        // Act
        val result = repository.getBackupInfo()

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SocialLoginError.BackupNotFound)
    }

    @Test
    fun `GIVEN the flag cannot be stored WHEN setSocialBackupCompleted THEN PersistenceFailed is returned`() =
        runTest {
            // Arrange
            coEvery { store.setCompleted(any(), any()) } throws IllegalStateException("no space left")

            // Act
            val result = repository.setSocialBackupCompleted(
                walletId = UserWalletId(stringValue = WALLET_ID),
                isCompleted = true,
            )

            // Assert
            assertThat(result.leftOrNull()).isInstanceOf(SocialLoginError.PersistenceFailed::class.java)
        }

    @Test
    fun `GIVEN the wallet is in the completed set WHEN isSocialBackupCompleted THEN it reads as true`() = runTest {
        // Arrange
        coEvery { store.getCompletedWalletIds() } returns flowOf(setOf(WALLET_ID))

        // Act
        val isCompleted = repository.isSocialBackupCompleted(UserWalletId(stringValue = WALLET_ID))

        // Assert
        assertThat(isCompleted).isTrue()
    }

    private fun stubList(vararg files: DriveFile) {
        coEvery { api.listFiles(any(), any(), any(), any()) } returns Response.success(
            DriveFileListResponse(files = files.toList()),
        )
    }

    private fun stubDownload(fileId: String, content: String) {
        coEvery { api.downloadFileContent(any(), fileId, any()) } returns Response.success(
            content.toResponseBody(MIME_TYPE_JSON.toMediaType()),
        )
    }

    private fun backupJson(walletId: String = WALLET_ID, version: Int = SocialBackupCipher.VERSION): String {
        val payload = requireNotNull(
            SocialBackupPayload.encode(mnemonic = MNEMONIC.toCharArray(), isPassphraseRequired = false),
        )
        val data = cipher.encrypt(
            payload = payload,
            recoveryKey = recoveryKey,
            walletId = walletId,
            createdAt = CREATED_AT,
        )
        return SocialBackupJson.encodeToString(data.copy(version = version))
    }

    private fun secret() = SocialBackupSecret(mnemonic = MNEMONIC.toCharArray(), isPassphraseRequired = false)

    private fun driveFile(id: String, createdTime: String = CREATED_AT) =
        DriveFile(id = id, name = BACKUP_FILE_NAME, createdTime = createdTime)

    private fun <T> errorResponse(code: Int): Response<T> =
        Response.error(code, "".toResponseBody(MIME_TYPE_JSON.toMediaType()))

    private fun RequestBody.asString(): String = Buffer().also { writeTo(it) }.readUtf8()

    private companion object {
        const val ACCESS_TOKEN = "token"
        const val BACKUP_FILE_NAME = "social-backup.json"
        const val MIME_TYPE_JSON = "application/json"
        const val NEW_FILE_ID = "new-file"
        const val OLD_FILE_ID = "old-file"
        const val WALLET_ID = "104E6A9ECD85FFDF5F94E2CE4AC90B8CF6AA47CC81A0FE213359D3604F18769B"
        const val OTHER_WALLET_ID = "204E6A9ECD85FFDF5F94E2CE4AC90B8CF6AA47CC81A0FE213359D3604F18769B"
        const val MNEMONIC = "one two three"
        const val CREATED_AT = "2026-08-10T10:15:30Z"
        const val CREATED_AT_MILLIS = 1_786_875_330_000L
        const val UNSUPPORTED_VERSION = 2
    }
}