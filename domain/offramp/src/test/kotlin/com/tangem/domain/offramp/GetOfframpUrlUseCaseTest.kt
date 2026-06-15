package com.tangem.domain.offramp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.repository.OfframpRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfframpUrlUseCaseTest {

    private val offrampRepository: OfframpRepository = mockk()
    private val useCase = GetOfframpUrlUseCase(offrampRepository)

    private val userWalletId = UserWalletId("011")
    private val currencyId = "bitcoin"
    private val requestId = "request-id-001"
    private val cryptoCurrency: CryptoCurrency = mockk {
        every { id } returns mockk { every { value } returns currencyId }
    }
    private val appCurrencyCode = "USD"
    private val walletAddress = "0x1234567890abcdef"
    private val expectedUrl = "https://moonpay.com/sell?address=$walletAddress"

    @BeforeEach
    fun resetMocks() {
        clearMocks(offrampRepository)
        coEvery { offrampRepository.registerPendingOfframp(any(), any()) } returns requestId
    }

    @Test
    fun `invoke should register request_id and return url when wallet address and url are available`() = runTest {
        // Arrange
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(walletAddress = walletAddress)
        coEvery {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
                requestId = requestId,
            )
        } returns expectedUrl

        // Act
        val result = useCase(userWalletId, cryptoCurrencyStatus, appCurrencyCode)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedUrl)

        coVerify(exactly = 1) { offrampRepository.registerPendingOfframp(userWalletId, currencyId) }
        coVerify(exactly = 1) {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
                requestId = requestId,
            )
        }
    }

    @Test
    fun `invoke should return WalletAddressNotFound error when network address is null`() = runTest {
        // Arrange
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(networkAddress = null)

        // Act
        val result = useCase(userWalletId, cryptoCurrencyStatus, appCurrencyCode)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetOfframpUrlUseCase.Error.WalletAddressNotFound)

        coVerify(exactly = 0) { offrampRepository.registerPendingOfframp(any(), any()) }
        coVerify(exactly = 0) { offrampRepository.getOfframpUrl(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke should return UrlNotAvailable error when repository returns null`() = runTest {
        // Arrange
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(walletAddress = walletAddress)
        coEvery {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
                requestId = requestId,
            )
        } returns null

        // Act
        val result = useCase(userWalletId, cryptoCurrencyStatus, appCurrencyCode)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetOfframpUrlUseCase.Error.UrlNotAvailable)

        coVerify(exactly = 1) {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
                requestId = requestId,
            )
        }
    }

    private fun createCryptoCurrencyStatus(
        walletAddress: String? = null,
        networkAddress: NetworkAddress? = null,
    ): CryptoCurrencyStatus {
        val resolvedNetworkAddress = networkAddress ?: walletAddress?.let { address ->
            mockk<NetworkAddress> {
                every { defaultAddress } returns mockk {
                    every { value } returns address
                }
            }
        }

        val statusValue: CryptoCurrencyStatus.Value = mockk {
            every { this@mockk.networkAddress } returns resolvedNetworkAddress
        }

        return mockk {
            every { currency } returns cryptoCurrency
            every { value } returns statusValue
        }
    }
}