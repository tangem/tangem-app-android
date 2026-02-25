package com.tangem.tap.data

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.network.exchangeServices.SellService
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultOfframpRepositoryTest {

    private val sellService: SellService = mockk()
    private val repository = DefaultOfframpRepository(sellService)

    private val cryptoCurrency: CryptoCurrency = mockk()
    private val fiatCurrencyCode = "USD"
    private val walletAddress = "0x1234567890abcdef"

    @BeforeEach
    fun setUp() {
        mockkObject(MutableAppThemeModeHolder)
    }

    @AfterEach
    fun tearDown() {
        clearMocks(sellService)
        unmockkObject(MutableAppThemeModeHolder)
    }

    @Test
    fun `getOfframpUrl should return url when sellService returns url with light theme`() {
        // Arrange
        val expectedUrl = "https://moonpay.com/sell?address=$walletAddress&theme=light"
        every { MutableAppThemeModeHolder.isDarkThemeActive } returns false
        every {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
            )
        } returns expectedUrl

        // Act
        val result = repository.getOfframpUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyCode = fiatCurrencyCode,
            walletAddress = walletAddress,
        )

        // Assert
        assertThat(result).isEqualTo(expectedUrl)

        verify(exactly = 1) {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
            )
        }
    }

    @Test
    fun `getOfframpUrl should return url when sellService returns url with dark theme`() {
        // Arrange
        val expectedUrl = "https://moonpay.com/sell?address=$walletAddress&theme=dark"
        every { MutableAppThemeModeHolder.isDarkThemeActive } returns true
        every {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = true,
            )
        } returns expectedUrl

        // Act
        val result = repository.getOfframpUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyCode = fiatCurrencyCode,
            walletAddress = walletAddress,
        )

        // Assert
        assertThat(result).isEqualTo(expectedUrl)

        verify(exactly = 1) {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = true,
            )
        }
    }

    @Test
    fun `getOfframpUrl should return null when sellService returns null`() {
        // Arrange
        every { MutableAppThemeModeHolder.isDarkThemeActive } returns false
        every {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
            )
        } returns null

        // Act
        val result = repository.getOfframpUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyCode = fiatCurrencyCode,
            walletAddress = walletAddress,
        )

        // Assert
        assertThat(result).isNull()

        verify(exactly = 1) {
            sellService.getUrl(
                cryptoCurrency = cryptoCurrency,
                fiatCurrencyName = fiatCurrencyCode,
                walletAddress = walletAddress,
                isDarkTheme = false,
            )
        }
    }
}
