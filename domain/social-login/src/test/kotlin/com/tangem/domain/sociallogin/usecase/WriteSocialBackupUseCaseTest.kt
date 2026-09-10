package com.tangem.domain.sociallogin.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialBackupInfo
import com.tangem.domain.sociallogin.models.SocialBackupSecret
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

internal class WriteSocialBackupUseCaseTest {

    private val socialBackupRepository: SocialBackupRepository = mockk()

    private val useCase = WriteSocialBackupUseCase(socialBackupRepository)

    private val walletId = UserWalletId(stringValue = "0011")
    private val otherWalletId = UserWalletId(stringValue = "0022")
    private val secret = SocialBackupSecret(mnemonic = "abandon".toCharArray(), isPassphraseRequired = false)
    private val recoveryKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 1 })

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialBackupRepository)
        coEvery { socialBackupRepository.getBackupInfo() } returns SocialLoginError.BackupNotFound.left()
        coEvery { socialBackupRepository.setSocialBackupCompleted(any(), any()) } returns Unit.right()
        coEvery { socialBackupRepository.writeBackup(walletId, any(), secret, recoveryKey) } returns Unit.right()
    }

    @Test
    fun `GIVEN empty folder WHEN invoke THEN backup is written AND no flag is cleared`() = runTest {
        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { socialBackupRepository.writeBackup(walletId, any(), secret, recoveryKey) }
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
    }

    @Test
    fun `GIVEN folder holds another wallet backup WHEN invoke THEN its flag is cleared before the write`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.getBackupInfo() } returns backupInfo(otherWalletId).right()

        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            socialBackupRepository.setSocialBackupCompleted(walletId = otherWalletId, isCompleted = false)
            socialBackupRepository.writeBackup(walletId, any(), secret, recoveryKey)
        }
    }

    @Test
    fun `GIVEN folder holds the same wallet backup WHEN invoke THEN no flag is cleared`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.getBackupInfo() } returns backupInfo(walletId).right()

        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
    }

    @Test
    fun `GIVEN unreadable file WHEN invoke THEN backup is still written`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.getBackupInfo() } returns SocialLoginError.InvalidBackupFile.left()

        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { socialBackupRepository.writeBackup(walletId, any(), secret, recoveryKey) }
    }

    @Test
    fun `GIVEN another wallet backup of an unknown version WHEN invoke THEN its flag is cleared`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.getBackupInfo() } returns backupInfo(otherWalletId, version = 99).right()

        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            socialBackupRepository.setSocialBackupCompleted(walletId = otherWalletId, isCompleted = false)
        }
    }

    @Test
    fun `GIVEN header did not arrive WHEN invoke THEN nothing is written`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.getBackupInfo() } returns SocialLoginError.CloudUnavailable.left()

        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.CloudUnavailable.left())
        coVerify(exactly = 0) { socialBackupRepository.writeBackup(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN old flag cannot be cleared WHEN invoke THEN nothing is written`() = runTest {
        // Arrange
        coEvery { socialBackupRepository.getBackupInfo() } returns backupInfo(otherWalletId).right()
        coEvery {
            socialBackupRepository.setSocialBackupCompleted(otherWalletId, false)
        } returns SocialLoginError.PersistenceFailed().left()

        // Act
        val actual = useCase(walletId = walletId, secret = secret, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PersistenceFailed().left())
        coVerify(exactly = 0) { socialBackupRepository.writeBackup(any(), any(), any(), any()) }
    }

    private fun backupInfo(walletId: UserWalletId, version: Int = 1) = SocialBackupInfo(
        walletId = walletId,
        createdAtMillis = 0L,
        version = version,
    )
}