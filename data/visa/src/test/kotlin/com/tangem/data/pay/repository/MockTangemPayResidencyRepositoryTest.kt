package com.tangem.data.pay.repository

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class MockTangemPayResidencyRepositoryTest {

    private val origin: TangemPayResidencyRepository = mockk()

    private val repository = MockTangemPayResidencyRepository(origin = origin)

    @Test
    fun `GIVEN seed WHEN getCountries THEN every country with a region list is flagged as having regions`() = runTest {
        // Act
        val actual = repository.getCountries().getOrNull().orEmpty()

        // Assert
        val withRegions = actual.filter(TangemPayCountry::hasRegions).map(TangemPayCountry::code)
        assertThat(withRegions).containsExactlyElementsIn(MockTangemPayResidencySeed.regions.keys)
        assertThat(actual.map(TangemPayCountry::code)).containsNoDuplicates()
    }

    @Test
    fun `GIVEN country with regions WHEN getRegions THEN seeded regions returned`() = runTest {
        // Act
        val actual = repository.getRegions(countryCode = "US")

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(MockTangemPayResidencySeed.regions.getValue("US"))
    }

    @Test
    fun `GIVEN lowercase code WHEN getRegions THEN still resolved`() = runTest {
        // Act
        val actual = repository.getRegions(countryCode = "ca")

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(MockTangemPayResidencySeed.regions.getValue("CA"))
    }

    @Test
    fun `GIVEN country without regions WHEN getRegions THEN not found`() = runTest {
        // Act
        val actual = repository.getRegions(countryCode = "MC")

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(VisaApiError.NotFound)
    }

    @Test
    fun `GIVEN seed WHEN getRegions for every flagged country THEN region codes are unique`() = runTest {
        MockTangemPayResidencySeed.regions.keys.forEach { code ->
            // Act
            val actual = repository.getRegions(countryCode = code).getOrNull().orEmpty()

            // Assert
            assertThat(actual.map(TangemPayRegion::code)).containsNoDuplicates()
            assertThat(actual).isNotEmpty()
        }
    }

    @Test
    fun `GIVEN customer check WHEN called THEN delegated to the real repository`() = runTest {
        // Arrange
        val expected = TangemPayEligibility(isAvailable = true, country = "US", region = "CA")
        coEvery { origin.checkEligibilityForCustomer(any(), any()) } returns Either.Right(expected)

        // Act
        val actual = repository.checkEligibilityForCustomer(userWalletId = WALLET_ID, product = PRODUCT)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(expected)
        coVerify(exactly = 1) { origin.checkEligibilityForCustomer(WALLET_ID, PRODUCT) }
    }

    @Test
    fun `GIVEN country check WHEN called THEN delegated to the real repository`() = runTest {
        // Arrange
        val expected = TangemPayEligibility(isAvailable = false, country = "MC", region = null)
        coEvery { origin.checkEligibilityForCountry(any(), any(), any()) } returns Either.Right(expected)

        // Act
        val actual = repository.checkEligibilityForCountry(
            product = PRODUCT,
            countryCode = "MC",
            regionCode = null,
        )

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(expected)
        coVerify(exactly = 1) { origin.checkEligibilityForCountry(PRODUCT, "MC", null) }
    }

    private companion object {
        val WALLET_ID = UserWalletId("0123456789abcdef")
        val PRODUCT = TangemPayProduct.BANKING_ACCOUNT
    }
}