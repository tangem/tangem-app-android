package com.tangem.domain.sociallogin.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DeleteSocialBackupAfterUpgradeUseCaseTest {

    private val socialBackupRepository: SocialBackupRepository = mockk()

    private val useCase = DeleteSocialBackupAfterUpgradeUseCase(socialBackupRepository = socialBackupRepository)

    private val walletId = UserWalletId(stringValue = "0011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialBackupRepository)
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns true
        coEvery { socialBackupRepository.setSocialBackupCompleted(walletId, false) } returns Unit.right()
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns Unit.right()
    }

    @Test
    fun `GIVEN a social backup WHEN invoke THEN the file is deleted before the flag is cleared`() = runTest {
        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(true.right())
        coVerifyOrder {
            socialBackupRepository.deleteBackup(walletId)
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false)
        }
    }

    @Test
    fun `GIVEN no social backup WHEN invoke THEN nothing is touched`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns false

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(false.right())
        coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
    }

    @Test
    fun `GIVEN the file cannot be deleted WHEN invoke THEN the error is returned and the flag is kept`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.CloudUnavailable.left())
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
    }

    @Test
    fun `GIVEN the flag cannot be stored WHEN invoke THEN the error is returned`() = runTest {
        // Arrange
        coEvery {
            socialBackupRepository.setSocialBackupCompleted(walletId, false)
        } returns SocialLoginError.PersistenceFailed().left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PersistenceFailed().left())
    }
}