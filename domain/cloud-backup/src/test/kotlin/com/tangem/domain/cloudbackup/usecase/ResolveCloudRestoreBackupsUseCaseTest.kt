package com.tangem.domain.cloudbackup.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.models.CloudBackupAccount
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.restore.CloudRestoreResult
import com.tangem.domain.cloudbackup.restore.CloudRestoreResultHolder
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ResolveCloudRestoreBackupsUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val holder = CloudRestoreResultHolder()
    private val useCase = ResolveCloudRestoreBackupsUseCase(cloudBackupRepository, holder)

    @BeforeEach
    fun resetMocks() {
        clearMocks(cloudBackupRepository)
        holder.clear()
        coEvery { cloudBackupRepository.signOut() } returns Unit
        coEvery { cloudBackupRepository.getAccountInfo(interactive = false) } returns ACCOUNT.right()
    }

    @Test
    fun `GIVEN backups found WHEN invoked THEN the account is signed out first and the result is parked`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true, validateContent = true) } returns listOf(BACKUP).right()

        // Act
        val actual = useCase()

        // Assert
        val expected = CloudRestoreResult(backups = listOf(BACKUP), accountEmail = ACCOUNT.email)
        assertThat(actual).isEqualTo(expected.right())
        assertThat(holder.result.value).isEqualTo(expected)
        coVerifyOrder {
            cloudBackupRepository.signOut()
            cloudBackupRepository.findBackups(interactive = true, validateContent = true)
            cloudBackupRepository.getAccountInfo(interactive = false)
        }
    }

    @Test
    fun `GIVEN no backups WHEN invoked THEN not found is raised and nothing is parked`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true, validateContent = true) } returns emptyList<CloudBackupInfo>().right()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.BackupNotFound.left())
        assertThat(holder.result.value).isNull()
        coVerify(exactly = 0) { cloudBackupRepository.getAccountInfo(any()) }
    }

    @Test
    fun `GIVEN account info unavailable WHEN invoked THEN the result carries no email`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true, validateContent = true) } returns listOf(BACKUP).right()
        coEvery { cloudBackupRepository.getAccountInfo(interactive = false) } returns CloudBackupError.AuthRequired.left()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(CloudRestoreResult(backups = listOf(BACKUP), accountEmail = null).right())
    }

    @Test
    fun `GIVEN listing fails WHEN invoked THEN the failure is passed through and nothing is parked`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true, validateContent = true) } returns CloudBackupError.NetworkError.left()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.NetworkError.left())
        assertThat(holder.result.value).isNull()
    }

    private companion object {
        val BACKUP = CloudBackupInfo(
            fileId = "file-1",
            backupName = "My Wallet",
            walletName = "My Wallet",
            createdAtMillis = 0L,
            walletId = "011",
        )
        val ACCOUNT = CloudBackupAccount(email = "user@gmail.com", displayName = null, photoUrl = null)
    }
}