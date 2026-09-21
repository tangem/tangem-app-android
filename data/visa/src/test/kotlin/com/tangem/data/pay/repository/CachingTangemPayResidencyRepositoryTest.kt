package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CachingTangemPayResidencyRepositoryTest {

    private val origin: TangemPayResidencyRepository = mockk()

    private var language = ENGLISH
    private lateinit var repository: CachingTangemPayResidencyRepository

    @BeforeEach
    fun createRepository() {
        clearMocks(origin)
        language = ENGLISH
        repository = CachingTangemPayResidencyRepository(origin = origin, currentLanguage = { language })
    }

    @Test
    fun `GIVEN empty cache WHEN countries requested twice THEN origin called once`() = runTest {
        // Arrange
        val countries = listOf(country())
        coEvery { origin.getCountries() } returns countries.right()

        // Act
        val first = repository.getCountries()
        val second = repository.getCountries()

        // Assert
        assertThat(first).isEqualTo(countries.right())
        assertThat(second).isEqualTo(countries.right())
        coVerify(exactly = 1) { origin.getCountries() }
    }

    @Test
    fun `GIVEN failing origin WHEN countries requested twice THEN origin called twice`() = runTest {
        // Arrange
        coEvery { origin.getCountries() } returns VisaApiError.ServerUnavailable.left()

        // Act
        val first = repository.getCountries()
        val second = repository.getCountries()

        // Assert
        assertThat(first).isEqualTo(VisaApiError.ServerUnavailable.left())
        assertThat(second).isEqualTo(VisaApiError.ServerUnavailable.left())
        coVerify(exactly = 2) { origin.getCountries() }
    }

    @Test
    fun `GIVEN cached regions WHEN same country requested THEN origin not called`() = runTest {
        // Arrange
        val regions = listOf(region())
        coEvery { origin.getRegions(US_COUNTRY_CODE) } returns regions.right()
        repository.getRegions(US_COUNTRY_CODE)

        // Act
        val actual = repository.getRegions(US_COUNTRY_CODE)

        // Assert
        assertThat(actual).isEqualTo(regions.right())
        coVerify(exactly = 1) { origin.getRegions(US_COUNTRY_CODE) }
    }

    @Test
    fun `GIVEN lowercase country code WHEN regions requested THEN cache hit`() = runTest {
        // Arrange
        val regions = listOf(region())
        coEvery { origin.getRegions(US_COUNTRY_CODE) } returns regions.right()
        repository.getRegions(US_COUNTRY_CODE)

        // Act
        val actual = repository.getRegions(US_COUNTRY_CODE.lowercase())

        // Assert
        assertThat(actual).isEqualTo(regions.right())
        coVerify(exactly = 1) { origin.getRegions(any()) }
    }

    @Test
    fun `GIVEN cached regions of one country WHEN another country requested THEN origin called for the new country`() =
        runTest {
            // Arrange
            val usRegions = listOf(region())
            val canadaRegions = listOf(region(code = "ON", name = "Ontario"))
            coEvery { origin.getRegions(US_COUNTRY_CODE) } returns usRegions.right()
            coEvery { origin.getRegions(CANADA_COUNTRY_CODE) } returns canadaRegions.right()
            repository.getRegions(US_COUNTRY_CODE)

            // Act
            val canada = repository.getRegions(CANADA_COUNTRY_CODE)
            val us = repository.getRegions(US_COUNTRY_CODE)

            // Assert
            assertThat(canada).isEqualTo(canadaRegions.right())
            assertThat(us).isEqualTo(usRegions.right())
            coVerify(exactly = 1) { origin.getRegions(CANADA_COUNTRY_CODE) }
            coVerify(exactly = 1) { origin.getRegions(US_COUNTRY_CODE) }
        }

    @Test
    fun `GIVEN cached lists WHEN device language changes THEN origin called again`() = runTest {
        // Arrange
        coEvery { origin.getCountries() } returns listOf(country()).right()
        coEvery { origin.getRegions(US_COUNTRY_CODE) } returns listOf(region()).right()
        repository.getCountries()
        repository.getRegions(US_COUNTRY_CODE)

        // Act
        language = GERMAN
        repository.getCountries()
        repository.getRegions(US_COUNTRY_CODE)

        // Assert
        coVerify(exactly = 2) { origin.getCountries() }
        coVerify(exactly = 2) { origin.getRegions(US_COUNTRY_CODE) }
    }

    @Test
    fun `GIVEN eligibility check WHEN performed THEN delegated to origin`() = runTest {
        // Arrange
        val eligibility = TangemPayEligibility(isAvailable = true, country = US_COUNTRY_CODE, region = null)
        coEvery {
            origin.checkEligibilityForCountry(TangemPayProduct.BANKING_ACCOUNT, US_COUNTRY_CODE, null)
        } returns eligibility.right()

        // Act
        val actual = repository.checkEligibilityForCountry(
            product = TangemPayProduct.BANKING_ACCOUNT,
            countryCode = US_COUNTRY_CODE,
            regionCode = null,
        )

        // Assert
        assertThat(actual).isEqualTo(eligibility.right())
        coVerify(exactly = 1) {
            origin.checkEligibilityForCountry(TangemPayProduct.BANKING_ACCOUNT, US_COUNTRY_CODE, null)
        }
    }

    @Test
    fun `GIVEN concurrent requests WHEN countries loaded THEN origin called once`() = runTest {
        // Arrange
        val countries = listOf(country())
        coEvery { origin.getCountries() } coAnswers {
            delay(timeMillis = FETCH_DELAY_MILLIS)
            countries.right()
        }

        // Act
        val first = async { repository.getCountries() }
        val second = async { repository.getCountries() }
        advanceUntilIdle()

        // Assert
        assertThat(first.await()).isEqualTo(countries.right())
        assertThat(second.await()).isEqualTo(countries.right())
        coVerify(exactly = 1) { origin.getCountries() }
    }

    private fun country(
        code: String = US_COUNTRY_CODE,
        name: String = "United States",
        flagUrl: String? = null,
        isSuggested: Boolean = true,
        hasRegions: Boolean = true,
    ) = TangemPayCountry(
        code = code,
        name = name,
        flagUrl = flagUrl,
        isSuggested = isSuggested,
        hasRegions = hasRegions,
    )

    private fun region(
        code: String = "NY",
        name: String = "New York",
        isSuggested: Boolean = false,
    ) = TangemPayRegion(code = code, name = name, isSuggested = isSuggested)

    private companion object {
        const val US_COUNTRY_CODE = "US"
        const val CANADA_COUNTRY_CODE = "CA"
        const val ENGLISH = "en"
        const val GERMAN = "de"
        const val FETCH_DELAY_MILLIS = 100L
    }
}