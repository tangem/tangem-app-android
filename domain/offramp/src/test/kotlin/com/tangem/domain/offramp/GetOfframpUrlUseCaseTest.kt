package com.tangem.domain.offramp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.offramp.repository.OfframpRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfframpUrlUseCaseTest {

    private val offrampRepository: OfframpRepository = mockk()
    private val useCase = GetOfframpUrlUseCase(offrampRepository)

    private val cryptoCurrency: CryptoCurrency = mockk()
    private val appCurrencyCode = "USD"
    private val walletAddress = "0x1234567890abcdef"
    private val expectedUrl = "https://moonpay.com/sell?address=$walletAddress"

    @BeforeEach
    fun resetMocks() {
        clearMocks(offrampRepository)
    }

    @Test
    fun `invoke should return url when wallet address and url are available`() {
        // Arrange
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(walletAddress = walletAddress)
        every {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
            )
        } returns expectedUrl

        // Act
        val result = useCase(cryptoCurrencyStatus, appCurrencyCode)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedUrl)

        verify(exactly = 1) {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
            )
        }
    }

    @Test
    fun `invoke should return WalletAddressNotFound error when network address is null`() {
        // Arrange
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(networkAddress = null)

        // Act
        val result = useCase(cryptoCurrencyStatus, appCurrencyCode)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetOfframpUrlUseCase.Error.WalletAddressNotFound)

        verify(exactly = 0) {
            offrampRepository.getOfframpUrl(any(), any(), any())
        }
    }

    @Test
    fun `invoke should return UrlNotAvailable error when repository returns null`() {
        // Arrange
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(walletAddress = walletAddress)
        every {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
            )
        } returns null

        // Act
        val result = useCase(cryptoCurrencyStatus, appCurrencyCode)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetOfframpUrlUseCase.Error.UrlNotAvailable)

        verify(exactly = 1) {
            offrampRepository.getOfframpUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyCode = appCurrencyCode,
                walletAddress = walletAddress,
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

