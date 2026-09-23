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

    private val useCase = DeleteSocialRecoveryUseCase(
        socialBackupRepository = socialBackupRepository,
        socialRecoveryRepository = socialRecoveryRepository,
    )

    private val walletId = UserWalletId(stringValue = "0011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialBackupRepository, socialRecoveryRepository)
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns true
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
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = false)
            socialRecoveryRepository.unbindWallet(walletId)
            socialBackupRepository.deleteBackup(walletId)
        }
    }

    @Test
    fun `GIVEN wallet has no social backup WHEN invoke THEN only the binding is dropped`() = runTest {
        // Arrange — a binding can outlive the flag, so it is still worth dropping
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns false

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { socialRecoveryRepository.unbindWallet(walletId) }
        // the flag is already false, and the cloud call would authorize the hidden Drive folder
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
        coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
    }

    @Test
    fun `GIVEN no social backup AND unbind fails WHEN invoke THEN the upgrade is not blocked`() = runTest {
        // Arrange — nothing was ever set up here, so a backend hiccup must not fail the teardown
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns false
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns SocialLoginError.NetworkError.left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { socialBackupRepository.deleteBackup(any()) }
    }

    @Test
    fun `GIVEN unbind fails WHEN invoke THEN the file is still removed and the error reported`() = runTest {
        // Arrange — the backend may be unreachable, or its authentication switched off altogether
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns SocialLoginError.NetworkError.left()

        // Act
        val actual = useCase(walletId)

        // Assert — without the file the identity has nothing left to restore from
        assertThat(actual).isEqualTo(SocialLoginError.NetworkError.left())
        coVerifyOrder {
            socialBackupRepository.setSocialBackupCompleted(walletId, false)
            socialBackupRepository.deleteBackup(walletId)
        }
    }

    @Test
    fun `GIVEN unbind AND file deletion fail WHEN invoke THEN the unbind error is reported`() = runTest {
        // Arrange
        coEvery { socialRecoveryRepository.unbindWallet(walletId) } returns SocialLoginError.NetworkError.left()
        coEvery { socialBackupRepository.deleteBackup(walletId) } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase(walletId)

        // Assert — a live binding matters more than a leftover file
        assertThat(actual).isEqualTo(SocialLoginError.NetworkError.left())
    }

    @Test
    fun `GIVEN flag cannot be stored WHEN invoke THEN nothing is torn down`() = runTest {
        // Arrange — a flag stuck at `true` would outlive the key and keep claiming a backup
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
    fun `GIVEN no social backup AND the flag cannot be stored WHEN invoke THEN the binding is dropped`() = runTest {
        // Arrange — a half-registered binding must not survive a local storage failure over a flag that
        // is already false
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns false
        coEvery {
            socialBackupRepository.setSocialBackupCompleted(walletId, false)
        } returns SocialLoginError.PersistenceFailed().left()

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { socialRecoveryRepository.unbindWallet(walletId) }
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