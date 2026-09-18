package com.tangem.domain.sociallogin.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RegisterSocialRecoveryUseCaseTest {

    private val socialRecoveryRepository: SocialRecoveryRepository = mockk()
    private val socialBackupRepository: SocialBackupRepository = mockk()

    private val useCase = RegisterSocialRecoveryUseCase(socialRecoveryRepository, socialBackupRepository)

    private val idToken = SocialProviderIdToken(value = "id-token", nonce = "nonce")
    private val walletId = UserWalletId(stringValue = "0011")
    private val recoveryKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 1 })

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialRecoveryRepository, socialBackupRepository)
        coEvery { socialBackupRepository.setSocialBackupCompleted(walletId, true) } returns Unit.right()
    }

    @Test
    fun `GIVEN key registered WHEN invoke THEN wallet is marked as socially backed up`() = runTest {
        // Arrange
        coEvery { socialRecoveryRepository.registerRecoveryKey(idToken, walletId, recoveryKey) } returns Unit.right()

        // Act
        val actual = useCase(idToken = idToken, walletId = walletId, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            socialBackupRepository.setSocialBackupCompleted(walletId = walletId, isCompleted = true)
        }
    }

    @Test
    fun `GIVEN registration fails WHEN invoke THEN wallet is not marked as socially backed up`() = runTest {
        // Arrange
        coEvery {
            socialRecoveryRepository.registerRecoveryKey(idToken, walletId, recoveryKey)
        } returns SocialLoginError.BindingConflict.left()

        // Act
        val actual = useCase(idToken = idToken, walletId = walletId, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.BindingConflict.left())
        coVerify(exactly = 0) { socialBackupRepository.setSocialBackupCompleted(any(), any()) }
    }

    @Test
    fun `GIVEN flag cannot be stored WHEN invoke THEN setup is not reported as complete`() = runTest {
        // Arrange
        coEvery { socialRecoveryRepository.registerRecoveryKey(idToken, walletId, recoveryKey) } returns Unit.right()
        coEvery {
            socialBackupRepository.setSocialBackupCompleted(walletId, true)
        } returns SocialLoginError.PersistenceFailed().left()

        // Act
        val actual = useCase(idToken = idToken, walletId = walletId, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.PersistenceFailed().left())
    }
}