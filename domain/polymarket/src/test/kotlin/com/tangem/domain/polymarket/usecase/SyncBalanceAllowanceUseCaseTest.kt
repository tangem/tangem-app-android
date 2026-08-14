package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncBalanceAllowanceUseCaseTest {

    private val repository: PolymarketRepository = mockk()

    private val useCase = SyncBalanceAllowanceUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN the sync succeeds WHEN invoked THEN returns Unit and calls the repository once`() = runTest {
        // Arrange
        coEvery { repository.syncBalanceAllowance(OWNER, CREDENTIALS) } returns Unit.right()

        // Act
        val actual = useCase(ownerAddress = OWNER, credentials = CREDENTIALS)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { repository.syncBalanceAllowance(OWNER, CREDENTIALS) }
    }

    @Test
    fun `GIVEN a transport failure WHEN invoked THEN maps to the Network onboarding error`() = runTest {
        // Arrange
        coEvery { repository.syncBalanceAllowance(OWNER, CREDENTIALS) } returns
            PolymarketAuthError.Network.left()

        // Act
        val actual = useCase(ownerAddress = OWNER, credentials = CREDENTIALS)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }

    @Test
    fun `GIVEN a rejected signature WHEN invoked THEN maps to the Auth onboarding error`() = runTest {
        // Arrange
        coEvery { repository.syncBalanceAllowance(OWNER, CREDENTIALS) } returns
            PolymarketAuthError.InvalidSignature.left()

        // Act
        val actual = useCase(ownerAddress = OWNER, credentials = CREDENTIALS)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketOnboardingError.Auth(cause = PolymarketAuthError.InvalidSignature).left(),
        )
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
    }
}