package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetPolymarketApiCredentialsUseCaseTest {

    private val credentialsStore: PolymarketCredentialsStore = mockk()

    private val useCase = GetPolymarketApiCredentialsUseCase(credentialsStore = credentialsStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(credentialsStore)
    }

    @Test
    fun `GIVEN credentials are stored WHEN invoke THEN returns them`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(OWNER) } returns CREDENTIALS

        // Act
        val actual = useCase(ownerAddress = OWNER)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(OWNER) } returns null

        // Act
        val actual = useCase(ownerAddress = OWNER)

        // Assert
        assertThat(actual).isNull()
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
    }
}