package com.tangem.domain.sociallogin.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DeleteSocialRecoveryUseCaseTest {

    private val socialBackupRepository: SocialBackupRepository = mockk()
    private val socialRecoveryRepository: SocialRecoveryRepository = mockk()

    private val useCase = DeleteSocialRecoveryUseCase(socialBackupRepository, socialRecoveryRepository)

    private val walletId = UserWalletId(stringValue = "0011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialBackupRepository, socialRecoveryRepository)
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns Unit.right()
        coEvery { socialBackupRepository.setSocialBackupCompleted(walletId, false) } returns Unit.right()
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns Unit.right()
    }

    @Test
    fun `GIVEN every step succeeds WHEN invoke THEN binding is dropped before the file is deleted`() = runTest {
        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            socialRecoveryRepository.unbindWallet(walletId)
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false)
            socialBackupRepository.deleteBackup(walletId)
        }
    }

    @Test
    fun `GIVEN unbind fails WHEN invoke THEN the backup is left usable`() = runTest {
        // Arrange
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns SocialLoginError.NetworkError.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.NetworkError.left())
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
        coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
    }

    @Test
    fun `GIVEN flag cannot be stored WHEN invoke THEN the file is kept`() = runTest {
        // Arrange
        coEvery {
            socialBackupRepository.setSocialBackupCompleted(walletId, false)
        } returns SocialLoginError.PersistenceFailed().left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PersistenceFailed().left())
        coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
    }

    @Test
    fun `GIVEN file deletion fails WHEN invoke THEN wallet no longer counts as socially backed up`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.CloudUnavailable.left())
        coVerify(exactly = 1) { socialRecoveryRepository.unbindWallet(walletId) }
        coVerify(exactly = 1) {
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false)
        }
    }
}