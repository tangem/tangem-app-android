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

internal class RevokeSocialRecoveryUseCaseTest {

    private val socialRecoveryRepository: SocialRecoveryRepository = mockk()
    private val socialBackupRepository: SocialBackupRepository = mockk()

    private val useCase = RevokeSocialRecoveryUseCase(
        socialRecoveryRepository = socialRecoveryRepository,
        socialBackupRepository = socialBackupRepository,
    )

    private val walletId = UserWalletId(stringValue = "0011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialRecoveryRepository, socialBackupRepository)
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns Unit.right()
        coEvery { socialBackupRepository.setSocialBackupCompleted(walletId, any()) } returns Unit.right()
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns Unit.right()
    }

    @Test
    fun `GIVEN every step succeeds WHEN invoke THEN the flag goes before the binding and the file`() = runTest {
        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false)
            socialRecoveryRepository.unbindWallet(walletId)
            socialBackupRepository.deleteBackup(walletId)
        }
    }

    @Test
    fun `GIVEN the flag cannot be cleared WHEN invoke THEN the error is returned and nothing else is touched`() =
        runTest {
            // Arrange
            coEvery {
                socialBackupRepository.setSocialBackupCompleted(walletId, false)
            } returns SocialLoginError.PersistenceFailed().left()

            // Act
            val actual = useCase(walletId)

            // Assert
            assertThat(actual).isEqualTo(SocialLoginError.PersistenceFailed().left())
            coVerify(exactly = 0) { socialRecoveryRepository.unbindWallet(any()) }
            coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
        }

    @Test
    fun `GIVEN unbind fails WHEN invoke THEN the flag is restored and the file is kept`() = runTest {
        // Arrange
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns SocialLoginError.NetworkError.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.NetworkError.left())
        coVerifyOrder {
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false)
            socialRecoveryRepository.unbindWallet(walletId)
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = true)
        }
        coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
    }

    @Test
    fun `GIVEN the file cannot be deleted WHEN invoke THEN the deletion still goes ahead`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }
}