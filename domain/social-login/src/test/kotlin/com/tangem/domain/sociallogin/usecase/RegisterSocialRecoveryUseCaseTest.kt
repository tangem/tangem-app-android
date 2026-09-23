package com.tangem.domain.sociallogin.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
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

    private val useCase = RegisterSocialRecoveryUseCase(socialRecoveryRepository)

    private val idToken = SocialProviderIdToken(value = "id-token", nonce = "nonce")
    private val walletId = UserWalletId(stringValue = "0011")
    private val recoveryKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 1 })

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialRecoveryRepository)
    }

    @Test
    fun `GIVEN repository accepts WHEN invoke THEN the binding is registered`() = runTest {
        // Arrange
        coEvery { socialRecoveryRepository.registerRecoveryKey(idToken, walletId, recoveryKey) } returns Unit.right()

        // Act
        val actual = useCase(idToken = idToken, walletId = walletId, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { socialRecoveryRepository.registerRecoveryKey(idToken, walletId, recoveryKey) }
    }

    @Test
    fun `GIVEN registration fails WHEN invoke THEN the failure is reported as is`() = runTest {
        // Arrange
        coEvery {
            socialRecoveryRepository.registerRecoveryKey(idToken, walletId, recoveryKey)
        } returns SocialLoginError.BindingConflict.left()

        // Act
        val actual = useCase(idToken = idToken, walletId = walletId, recoveryKey = recoveryKey)

        // Assert
        assertThat(actual).isEqualTo(SocialLoginError.BindingConflict.left())
    }
}