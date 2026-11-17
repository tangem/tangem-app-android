package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.tangempay.TangemPayEligibility
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.tangempay.error.TangemPayApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetTangemPayProductsAvailabilityForCountryUseCaseTest {

    private val checkTangemPayEligibilityUseCase: CheckTangemPayEligibilityUseCase = mockk()

    private val useCase = GetTangemPayProductsAvailabilityForCountryUseCase(
        checkTangemPayEligibilityUseCase = checkTangemPayEligibilityUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkTangemPayEligibilityUseCase)
    }

    @Test
    fun `GIVEN eligibility checks succeed WHEN invoke THEN returns only available products`() = runTest {
        // Arrange
        coEvery {
            checkTangemPayEligibilityUseCase(TangemPayProduct.BANKING_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        } returns eligibility(isAvailable = true).right()
        coEvery {
            checkTangemPayEligibilityUseCase(TangemPayProduct.PAYMENT_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        } returns eligibility(isAvailable = false).right()

        // Act
        val actual = useCase(countryCode = COUNTRY_CODE, regionCode = REGION_CODE)

        // Assert
        assertThat(actual).isEqualTo(setOf(TangemPayProduct.BANKING_ACCOUNT).right())
        coVerify(exactly = 1) {
            checkTangemPayEligibilityUseCase(TangemPayProduct.BANKING_ACCOUNT, COUNTRY_CODE, REGION_CODE)
            checkTangemPayEligibilityUseCase(TangemPayProduct.PAYMENT_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        }
    }

    @Test
    fun `GIVEN one eligibility check fails WHEN invoke THEN returns Left with its error`() = runTest {
        // Arrange
        coEvery {
            checkTangemPayEligibilityUseCase(TangemPayProduct.BANKING_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        } returns TangemPayApiError.Unspecified.left()
        coEvery {
            checkTangemPayEligibilityUseCase(TangemPayProduct.PAYMENT_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        } returns eligibility(isAvailable = true).right()

        // Act
        val actual = useCase(countryCode = COUNTRY_CODE, regionCode = REGION_CODE)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.Unspecified.left())
    }

    private fun eligibility(isAvailable: Boolean) = TangemPayEligibility(
        isAvailable = isAvailable,
        country = COUNTRY_CODE,
        region = REGION_CODE,
    )

    private companion object {
        const val COUNTRY_CODE = "US"
        const val REGION_CODE = "CA"
    }
}