package com.tangem.domain.cloudbackup.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DeleteCloudBackupWithRetryUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()

    private val fileId = "file-1"
    private val error: CloudBackupError = CloudBackupError.NetworkError

    @BeforeEach
    fun resetMocks() {
        clearMocks(cloudBackupRepository)
    }

    @Test
    fun `GIVEN deleteBackup returns Right WHEN invoke THEN result Right AND called once with no delay`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.deleteBackup(fileId) } returns Unit.right()

        // Act
        val result = createUseCase().invoke(fileId)
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(Unit.right())
        assertThat(testScheduler.currentTime).isEqualTo(0L)
        coVerify(exactly = 1) { cloudBackupRepository.deleteBackup(fileId) }
    }

    @Test
    fun `GIVEN deleteBackup fails twice then Right WHEN invoke THEN result Right AND called three times within 60s`() =
        runTest {
            // Arrange
            coEvery { cloudBackupRepository.deleteBackup(fileId) } returnsMany listOf(
                error.left(),
                error.left(),
                Unit.right(),
            )

            // Act
            val result = createUseCase().invoke(fileId)
            advanceUntilIdle()

            // Assert
            assertThat(result).isEqualTo(Unit.right())
            assertThat(testScheduler.currentTime).isEqualTo(SIXTY_SECONDS_MILLIS)
            coVerify(exactly = 3) { cloudBackupRepository.deleteBackup(fileId) }
        }

    @Test
    fun `GIVEN non-transient error WHEN invoke THEN not retried AND returns Left immediately`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.deleteBackup(fileId) } returns CloudBackupError.AuthPermissionsMissing.left()

        // Act
        val result = createUseCase().invoke(fileId)
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(CloudBackupError.AuthPermissionsMissing.left())
        assertThat(testScheduler.currentTime).isEqualTo(0L)
        coVerify(exactly = 1) { cloudBackupRepository.deleteBackup(fileId) }
    }

    @Test
    fun `GIVEN deleteBackup always Left WHEN invoke THEN result Left AND called three times across 60s`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.deleteBackup(fileId) } returns error.left()

        // Act
        val result = createUseCase().invoke(fileId)
        advanceUntilIdle()

        // Assert
        assertThat(result).isEqualTo(error.left())
        assertThat(testScheduler.currentTime).isEqualTo(SIXTY_SECONDS_MILLIS)
        coVerify(exactly = 3) { cloudBackupRepository.deleteBackup(fileId) }
    }

    @Test
    fun `GIVEN maxAttempts 0 WHEN invoke THEN throws AND never calls deleteBackup`() = runTest {
        // Act
        val exception = runCatching { createUseCase().invoke(fileId, maxAttempts = 0) }.exceptionOrNull()

        // Assert
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { cloudBackupRepository.deleteBackup(any()) }
    }

    private fun kotlinx.coroutines.test.TestScope.createUseCase(): DeleteCloudBackupWithRetryUseCase {
        val dispatchers = TestingCoroutineDispatcherProvider(io = StandardTestDispatcher(testScheduler))
        return DeleteCloudBackupWithRetryUseCase(cloudBackupRepository, dispatchers)
    }

    private companion object {
        const val SIXTY_SECONDS_MILLIS = 60_000L
    }
}