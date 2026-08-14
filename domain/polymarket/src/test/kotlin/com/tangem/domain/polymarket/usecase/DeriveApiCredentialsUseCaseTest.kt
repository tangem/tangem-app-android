package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DeriveApiCredentialsUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()
    private val credentialsStore: PolymarketCredentialsStore = mockk()

    private val useCase = DeriveApiCredentialsUseCase(
        polymarketRepository = polymarketRepository,
        credentialsStore = credentialsStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository, credentialsStore)
        coEvery { credentialsStore.store(any(), any()) } returns Unit
    }

    @Test
    fun `GIVEN credentials are already stored WHEN invoke THEN returns them without calling the service`() =
        runTest {
            // Arrange
            coEvery { credentialsStore.get(USER_WALLET_ID) } returns CREDENTIALS

            // Act
            val actual = useCase(userWalletId = USER_WALLET_ID, ownerAddress = OWNER, l1Signature = SIGNATURE, timestamp = TIMESTAMP)

            // Assert
            assertThat(actual).isEqualTo(CREDENTIALS.right())
            coVerify(exactly = 0) { polymarketRepository.deriveApiCredentials(any()) }
            coVerify(exactly = 0) { polymarketRepository.createApiCredentials(any()) }
        }

    @Test
    fun `GIVEN nothing is stored WHEN invoke THEN derives with the L1 headers and persists the result`() = runTest {
        // Arrange
        val headers = slot<PolymarketL1Headers>()
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null
        coEvery { polymarketRepository.deriveApiCredentials(capture(headers)) } returns CREDENTIALS.right()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, ownerAddress = OWNER, l1Signature = SIGNATURE, timestamp = TIMESTAMP)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS.right())
        assertThat(headers.captured).isEqualTo(
            PolymarketL1Headers(address = OWNER, signature = SIGNATURE, timestamp = TIMESTAMP, nonce = "0"),
        )
        coVerify(exactly = 1) { credentialsStore.store(USER_WALLET_ID, CREDENTIALS) }
    }

    @Test
    fun `GIVEN no key exists yet WHEN invoke THEN creates one with the same headers and persists it`() = runTest {
        // Arrange
        val deriveHeaders = slot<PolymarketL1Headers>()
        val createHeaders = slot<PolymarketL1Headers>()
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null
        coEvery { polymarketRepository.deriveApiCredentials(capture(deriveHeaders)) } returns
            PolymarketAuthError.KeyNotFound.left()
        coEvery { polymarketRepository.createApiCredentials(capture(createHeaders)) } returns CREDENTIALS.right()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, ownerAddress = OWNER, l1Signature = SIGNATURE, timestamp = TIMESTAMP)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS.right())
        assertThat(createHeaders.captured).isEqualTo(deriveHeaders.captured)
        coVerifyOrder {
            polymarketRepository.deriveApiCredentials(any())
            polymarketRepository.createApiCredentials(any())
            credentialsStore.store(USER_WALLET_ID, CREDENTIALS)
        }
    }

    @Test
    fun `GIVEN the signature is rejected WHEN invoke THEN does not try to create a key`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null
        coEvery { polymarketRepository.deriveApiCredentials(any()) } returns
            PolymarketAuthError.InvalidSignature.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, ownerAddress = OWNER, l1Signature = SIGNATURE, timestamp = TIMESTAMP)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketOnboardingError.Auth(PolymarketAuthError.InvalidSignature).left(),
        )
        coVerify(exactly = 0) { polymarketRepository.createApiCredentials(any()) }
        coVerify(exactly = 0) { credentialsStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN the service is unreachable WHEN invoke THEN returns Network`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null
        coEvery { polymarketRepository.deriveApiCredentials(any()) } returns PolymarketAuthError.Network.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, ownerAddress = OWNER, l1Signature = SIGNATURE, timestamp = TIMESTAMP)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
        coVerify(exactly = 0) { polymarketRepository.createApiCredentials(any()) }
    }

    @Test
    fun `GIVEN creation also fails WHEN invoke THEN wraps that error and stores nothing`() = runTest {
        // Arrange
        coEvery { credentialsStore.get(USER_WALLET_ID) } returns null
        coEvery { polymarketRepository.deriveApiCredentials(any()) } returns PolymarketAuthError.KeyNotFound.left()
        coEvery { polymarketRepository.createApiCredentials(any()) } returns PolymarketAuthError.RateLimited.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID, ownerAddress = OWNER, l1Signature = SIGNATURE, timestamp = TIMESTAMP)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Auth(PolymarketAuthError.RateLimited).left())
        coVerify(exactly = 0) { credentialsStore.store(any(), any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val SIGNATURE = "0xaa"
        const val TIMESTAMP = "1735689600"
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
    }
}