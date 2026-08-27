package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupStatus
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCloudBackupStatusUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()

    private val useCase = GetCloudBackupStatusUseCase(cloudBackupRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(cloudBackupRepository)
    }

    @Test
    fun `GIVEN wallet is not backed up WHEN invoke THEN Incomplete without cloud lookup`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns false

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CloudBackupStatus.Incomplete)
        coVerify(exactly = 0) { cloudBackupRepository.findBackups(any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: TestModel) = runTest {
        // Arrange
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns true
        coEvery { cloudBackupRepository.findBackups(any(), any()) } returns model.backups

        // Act
        val actual = useCase(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class TestModel(
        val backups: Either<CloudBackupError, List<CloudBackupInfo>>,
        val expected: CloudBackupStatus,
    )

    private fun provideTestModels() = listOf(
        TestModel(
            backups = listOf(backupInfo(walletId = WALLET_ID)).right(),
            expected = CloudBackupStatus.Done,
        ),
        TestModel(
            backups = listOf(backupInfo(walletId = "other-wallet")).right(),
            expected = CloudBackupStatus.ActionRequired,
        ),
        TestModel(
            backups = emptyList<CloudBackupInfo>().right(),
            expected = CloudBackupStatus.ActionRequired,
        ),
        TestModel(
            backups = CloudBackupError.AuthRequired.left(),
            expected = CloudBackupStatus.ActionRequired,
        ),
        TestModel(
            backups = CloudBackupError.AuthPermissionsMissing.left(),
            expected = CloudBackupStatus.ActionRequired,
        ),
        TestModel(
            backups = CloudBackupError.NetworkError.left(),
            expected = CloudBackupStatus.ActionRequired,
        ),
    )

    private fun backupInfo(walletId: String?) = CloudBackupInfo(
        fileId = "file-1",
        walletName = "Wallet",
        createdAtMillis = 0L,
        walletId = walletId,
    )

    private companion object {
        const val WALLET_ID = "wallet-1"
    }
}