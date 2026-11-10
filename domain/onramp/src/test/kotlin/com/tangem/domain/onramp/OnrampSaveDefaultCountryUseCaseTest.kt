package com.tangem.domain.onramp

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

internal class OnrampSaveDefaultCountryUseCaseTest {

    private val repository: OnrampRepository = mockk(relaxUnitFun = true)
    private val errorResolver: OnrampErrorResolver = mockk()

    private val useCase = OnrampSaveDefaultCountryUseCase(repository, errorResolver)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, errorResolver)
    }

    @Test
    fun `GIVEN user selected currency WHEN country saved THEN selected currency kept`() = runTest {
        // Arrange
        coEvery { repository.getSelectedCurrencySync() } returns USD

        // Act
        useCase(COUNTRY)

        // Assert
        coVerify(exactly = 1) { repository.saveDefaultCountry(COUNTRY.copy(defaultCurrency = USD)) }
    }

    @Test
    fun `GIVEN no selected currency WHEN country saved THEN country default currency used`() = runTest {
        // Arrange
        coEvery { repository.getSelectedCurrencySync() } returns null

        // Act
        useCase(COUNTRY)

        // Assert
        coVerify(exactly = 1) { repository.saveDefaultCountry(COUNTRY) }
    }

    private companion object {
        val USD = OnrampCurrency(name = "US Dollar", code = "USD", image = null, precision = 2, unit = "$")
        val EUR = OnrampCurrency(name = "Euro", code = "EUR", image = null, precision = 2, unit = "€")
        val COUNTRY = OnrampCountry(
            id = "DE",
            name = "Germany",
            code = "DE",
            image = "https://flag.png",
            alpha3 = "DEU",
            continent = "Europe",
            defaultCurrency = EUR,
            isOnrampAvailable = true,
        )
    }
}