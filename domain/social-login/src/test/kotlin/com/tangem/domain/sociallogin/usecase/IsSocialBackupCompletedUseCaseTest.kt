package com.tangem.domain.sociallogin.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IsSocialBackupCompletedUseCaseTest {

    private val socialBackupRepository: SocialBackupRepository = mockk()
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles = mockk()

    private val useCase = IsSocialBackupCompletedUseCase(socialBackupRepository, socialLoginFeatureToggles)

    private val walletId = UserWalletId(stringValue = "0011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(socialBackupRepository, socialLoginFeatureToggles)
    }

    @Test
    fun `GIVEN feature enabled AND backup completed WHEN invoke THEN returns true`() = runTest {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns true
        coEvery { socialBackupRepository.isSocialBackupCompleted(walletId) } returns true

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN feature disabled AND flag stored WHEN invoke THEN returns false`() = runTest {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns false

        // Act
        val actual = useCase(walletId)

        // Assert
        assertThat(actual).isFalse()
        coVerify(exactly = 0) { socialBackupRepository.isSocialBackupCompleted(any()) }
    }

    @Test
    fun `GIVEN feature disabled AND flag stored WHEN flow THEN emits false`() = runTest {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns false

        // Act
        val actual = useCase.flow(walletId).first()

        // Assert
        assertThat(actual).isFalse()
        verify(exactly = 0) { socialBackupRepository.isSocialBackupCompletedFlow(any()) }
    }

    @Test
    fun `GIVEN feature enabled WHEN flow THEN emits the stored flag`() = runTest {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns true
        every { socialBackupRepository.isSocialBackupCompletedFlow(walletId) } returns flowOf(true)

        // Act
        val actual = useCase.flow(walletId).first()

        // Assert
        assertThat(actual).isTrue()
    }
}