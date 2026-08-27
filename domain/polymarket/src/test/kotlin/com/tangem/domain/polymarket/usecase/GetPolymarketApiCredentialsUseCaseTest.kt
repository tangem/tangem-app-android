package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
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
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
    }
}