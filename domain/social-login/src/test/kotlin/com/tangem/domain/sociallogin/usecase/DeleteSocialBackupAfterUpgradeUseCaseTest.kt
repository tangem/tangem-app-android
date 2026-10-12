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
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns Unit.right()
    }

    @Test
    fun `GIVEN a social backup WHEN invoke THEN the file is deleted without touching the mark`() = runTest {
        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { socialBackupRepository.deleteBackup(walletId) }
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
    }

    @Test
    fun `GIVEN the file cannot be deleted WHEN invoke THEN the error is returned`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.CloudUnavailable.left())
    }
}