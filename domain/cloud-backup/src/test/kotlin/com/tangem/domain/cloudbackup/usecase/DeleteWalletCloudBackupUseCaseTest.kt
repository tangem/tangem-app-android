package com.tangem.domain.cloudbackup.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DeleteWalletCloudBackupUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val deleteCloudBackupWithRetryUseCase: DeleteCloudBackupWithRetryUseCase = mockk()

    private val useCase = DeleteWalletCloudBackupUseCase(cloudBackupRepository, deleteCloudBackupWithRetryUseCase)

    @BeforeEach
    fun resetMocks() {
        clearMocks(cloudBackupRepository, deleteCloudBackupWithRetryUseCase)
        coEvery { deleteCloudBackupWithRetryUseCase(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN matching backup WHEN invoke THEN its file is deleted`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups() } returns
            listOf(backup(fileId = "file-2", walletId = "999"), backup(fileId = "file-1", walletId = WALLET_ID)).right()

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { deleteCloudBackupWithRetryUseCase("file-1") }
    }

    @Test
    fun `GIVEN no matching backup WHEN invoke THEN nothing is deleted`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = "999")).right()

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
    }

    @Test
    fun `GIVEN backups cannot be listed WHEN invoke THEN error is returned AND nothing is deleted`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups() } returns CloudBackupError.NetworkError.left()

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.NetworkError.left())
        coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
    }

    @Test
    fun `GIVEN deletion fails WHEN invoke THEN error is returned`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = WALLET_ID)).right()
        coEvery { deleteCloudBackupWithRetryUseCase(any()) } returns CloudBackupError.NetworkError.left()

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.NetworkError.left())
    }

    private fun backup(fileId: String = "file-1", walletId: String?) = CloudBackupInfo(
        fileId = fileId,
        walletName = "wallet",
        createdAtMillis = 0L,
        walletId = walletId,
    )

    private companion object {
        const val WALLET_ID = "011"
    }
}