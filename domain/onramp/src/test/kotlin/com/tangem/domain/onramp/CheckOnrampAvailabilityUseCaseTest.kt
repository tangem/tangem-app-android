package com.tangem.domain.onramp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampAvailability
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CheckOnrampAvailabilityUseCaseTest {

    private val repository: OnrampRepository = mockk(relaxUnitFun = true)
    private val errorResolver: OnrampErrorResolver = mockk()
    private val userWallet: UserWallet = mockk()

    private val useCase = CheckOnrampAvailabilityUseCase(repository, errorResolver)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, errorResolver)
        coEvery { repository.fetchPaymentMethodsIfAbsent(userWallet) } returns Unit
        coEvery { repository.getDefaultCountrySync() } returns null
        coEvery { repository.getSelectedCurrencySync() } returns null
    }

    @Test
    fun `GIVEN no saved country and supported IP country WHEN invoke THEN country saved and available`() = runTest {
        // Arrange
        val ipCountry = createCountry(isOnrampAvailable = true, currency = EUR)
        coEvery { repository.getCountryByIp(userWallet) } returns ipCountry
        coEvery { repository.fetchCountries(userWallet) } returns listOf(ipCountry)

        // Act
        val actual = useCase(userWallet, isCountryByIpEnabled = true)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.Available(country = ipCountry, currency = EUR))
        coVerify(exactly = 1) { repository.saveDefaultCountry(ipCountry) }
    }

    @Test
    fun `GIVEN user selected currency WHEN IP country detected THEN selected currency wins over country default`() =
        runTest {
            // Arrange
            val ipCountry = createCountry(isOnrampAvailable = true, currency = EUR)
            coEvery { repository.getCountryByIp(userWallet) } returns ipCountry
            coEvery { repository.getSelectedCurrencySync() } returns USD
            coEvery { repository.fetchCountries(userWallet) } returns listOf(ipCountry)

            // Act
            val actual = useCase(userWallet, isCountryByIpEnabled = true)

            // Assert
            val expectedCountry = ipCountry.copy(defaultCurrency = USD)
            assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.Available(expectedCountry, USD))
            coVerify(exactly = 1) { repository.saveDefaultCountry(expectedCountry) }
        }

    @Test
    fun `GIVEN IP country without onramp WHEN invoke THEN not supported`() = runTest {
        // Arrange
        val ipCountry = createCountry(isOnrampAvailable = false, currency = EUR)
        coEvery { repository.getCountryByIp(userWallet) } returns ipCountry
        coEvery { repository.fetchCountries(userWallet) } returns listOf(ipCountry)

        // Act
        val actual = useCase(userWallet, isCountryByIpEnabled = true)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.NotSupported(ipCountry))
    }

    @Test
    fun `GIVEN country chosen manually WHEN invoke THEN IP not requested and chosen country kept`() = runTest {
        // Arrange
        val chosen = createCountry(id = "chosen", isOnrampAvailable = true, currency = USD)
        coEvery { repository.getDefaultCountrySync() } returns chosen
        coEvery { repository.fetchCountries(userWallet) } returns listOf(chosen)

        // Act
        val actual = useCase(userWallet, isCountryByIpEnabled = true)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.Available(country = chosen, currency = USD))
        coVerify(exactly = 0) { repository.getCountryByIp(any(), any()) }
        coVerify(exactly = 0) { repository.saveDefaultCountry(any()) }
    }

    @Test
    fun `GIVEN saved supported country WHEN countries list says it is unsupported THEN not supported and refreshed country saved`() =
        runTest {
            // Arrange
            val saved = createCountry(id = "chosen", isOnrampAvailable = true, currency = USD)
            val listed = saved.copy(isOnrampAvailable = false)
            coEvery { repository.getDefaultCountrySync() } returns saved
            coEvery { repository.fetchCountries(userWallet) } returns listOf(listed)

            // Act
            val actual = useCase(userWallet, isCountryByIpEnabled = true)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.NotSupported(listed))
            coVerify(exactly = 1) { repository.saveDefaultCountry(listed) }
        }

    @Test
    fun `GIVEN saved country and user selected currency WHEN invoke THEN available with selected currency`() = runTest {
        // Arrange
        val saved = createCountry(id = "chosen", isOnrampAvailable = true, currency = EUR)
        coEvery { repository.getDefaultCountrySync() } returns saved
        coEvery { repository.getSelectedCurrencySync() } returns USD
        coEvery { repository.fetchCountries(userWallet) } returns listOf(saved)

        // Act
        val actual = useCase(userWallet, isCountryByIpEnabled = true)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.Available(country = saved, currency = USD))
    }

    @Test
    fun `GIVEN IP says available but countries list says not WHEN invoke THEN not supported and validated country saved`() =
        runTest {
            // Arrange
            val ipCountry = createCountry(isOnrampAvailable = true, currency = EUR)
            val listed = ipCountry.copy(isOnrampAvailable = false)
            coEvery { repository.getCountryByIp(userWallet) } returns ipCountry
            coEvery { repository.fetchCountries(userWallet) } returns listOf(listed)

            // Act
            val actual = useCase(userWallet, isCountryByIpEnabled = true)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.NotSupported(listed))
            coVerify(exactly = 1) { repository.saveDefaultCountry(listed) }
        }

    @Test
    fun `GIVEN IP says unavailable but countries list says available WHEN invoke THEN available and validated country saved`() =
        runTest {
            // Arrange
            val ipCountry = createCountry(isOnrampAvailable = false, currency = EUR)
            val listed = ipCountry.copy(isOnrampAvailable = true)
            coEvery { repository.getCountryByIp(userWallet) } returns ipCountry
            coEvery { repository.fetchCountries(userWallet) } returns listOf(listed)

            // Act
            val actual = useCase(userWallet, isCountryByIpEnabled = true)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.Available(listed, EUR))
            coVerify(exactly = 1) { repository.saveDefaultCountry(listed) }
        }

    @Test
    fun `GIVEN toggle off and no saved country WHEN invoke THEN residency confirmation requested and nothing saved`() =
        runTest {
            // Arrange
            val ipCountry = createCountry(isOnrampAvailable = true, currency = EUR)
            coEvery { repository.getCountryByIp(userWallet) } returns ipCountry

            // Act
            val actual = useCase(userWallet)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.ConfirmResidency(ipCountry))
            coVerify(exactly = 0) { repository.saveDefaultCountry(any()) }
            coVerify(exactly = 0) { repository.fetchCountries(any()) }
        }

    @Test
    fun `GIVEN toggle off and saved available country WHEN invoke THEN available with saved currency`() = runTest {
        // Arrange
        val saved = createCountry(isOnrampAvailable = true, currency = EUR)
        coEvery { repository.getDefaultCountrySync() } returns saved
        coEvery { repository.fetchCountries(userWallet) } returns listOf(saved)
        coEvery { repository.getDefaultCurrencySync() } returns USD

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.Available(country = saved, currency = USD))
        coVerify(exactly = 0) { repository.getCountryByIp(any()) }
    }

    @Test
    fun `GIVEN toggle off and saved country no longer available WHEN invoke THEN not supported`() = runTest {
        // Arrange
        val saved = createCountry(isOnrampAvailable = true, currency = EUR)
        val listed = saved.copy(isOnrampAvailable = false)
        coEvery { repository.getDefaultCountrySync() } returns saved
        coEvery { repository.fetchCountries(userWallet) } returns listOf(listed)

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(OnrampAvailability.NotSupported(listed))
    }

    private fun createCountry(
        id: String = "ip",
        isOnrampAvailable: Boolean,
        currency: OnrampCurrency,
    ) = OnrampCountry(
        id = id,
        name = "Country $id",
        code = "CC",
        image = "https://flag.png",
        alpha3 = "CCC",
        continent = "Europe",
        defaultCurrency = currency,
        isOnrampAvailable = isOnrampAvailable,
    )

    private companion object {
        val EUR = OnrampCurrency(name = "Euro", code = "EUR", image = null, precision = 2, unit = "€")
        val USD = OnrampCurrency(name = "US Dollar", code = "USD", image = null, precision = 2, unit = "$")
    }
}