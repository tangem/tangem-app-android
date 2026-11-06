package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetTangemPayProductsAvailabilityForCountryUseCaseTest {

    private val checkTangemPayEligibilityUseCase: CheckTangemPayEligibilityUseCase = mockk()
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()

    private val useCase = GetTangemPayProductsAvailabilityForCountryUseCase(
        checkTangemPayEligibilityUseCase = checkTangemPayEligibilityUseCase,
        moneyAccountFeatureToggles = moneyAccountFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkTangemPayEligibilityUseCase, moneyAccountFeatureToggles)
        every { moneyAccountFeatureToggles.isMoneyAccountsEnabled } returns true
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
        } returns VisaApiError.Unspecified.left()
        coEvery {
            checkTangemPayEligibilityUseCase(TangemPayProduct.PAYMENT_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        } returns eligibility(isAvailable = true).right()

        // Act
        val actual = useCase(countryCode = COUNTRY_CODE, regionCode = REGION_CODE)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN money accounts disabled WHEN invoke THEN banking account not checked`() = runTest {
        // Arrange
        every { moneyAccountFeatureToggles.isMoneyAccountsEnabled } returns false
        coEvery {
            checkTangemPayEligibilityUseCase(TangemPayProduct.PAYMENT_ACCOUNT, COUNTRY_CODE, REGION_CODE)
        } returns eligibility(isAvailable = true).right()

        // Act
        val actual = useCase(countryCode = COUNTRY_CODE, regionCode = REGION_CODE)

        // Assert
        assertThat(actual).isEqualTo(setOf(TangemPayProduct.PAYMENT_ACCOUNT).right())
        coVerify(exactly = 0) { checkTangemPayEligibilityUseCase(TangemPayProduct.BANKING_ACCOUNT, any(), any()) }
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