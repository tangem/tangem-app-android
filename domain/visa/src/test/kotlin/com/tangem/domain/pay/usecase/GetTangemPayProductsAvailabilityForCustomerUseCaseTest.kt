package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
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

internal class GetTangemPayProductsAvailabilityForCustomerUseCaseTest {

    private val checkTangemPayEligibilityForCustomerUseCase: CheckTangemPayEligibilityForCustomerUseCase = mockk()
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()

    private val useCase = GetTangemPayProductsAvailabilityForCustomerUseCase(
        checkTangemPayEligibilityForCustomerUseCase = checkTangemPayEligibilityForCustomerUseCase,
        moneyAccountFeatureToggles = moneyAccountFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkTangemPayEligibilityForCustomerUseCase, moneyAccountFeatureToggles)
        every { moneyAccountFeatureToggles.isMoneyAccountsEnabled } returns true
    }

    @Test
    fun `GIVEN eligibility checks succeed WHEN invoke THEN returns only available products`() = runTest {
        // Arrange
        coEvery {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.BANKING_ACCOUNT)
        } returns eligibility(isAvailable = false).right()
        coEvery {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.PAYMENT_ACCOUNT)
        } returns eligibility(isAvailable = true).right()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(setOf(TangemPayProduct.PAYMENT_ACCOUNT).right())
        coVerify(exactly = 1) {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.BANKING_ACCOUNT)
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.PAYMENT_ACCOUNT)
        }
    }

    @Test
    fun `GIVEN one eligibility check fails WHEN invoke THEN returns Left with its error`() = runTest {
        // Arrange
        coEvery {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.BANKING_ACCOUNT)
        } returns eligibility(isAvailable = true).right()
        coEvery {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.PAYMENT_ACCOUNT)
        } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN money accounts disabled WHEN invoke THEN banking account not checked`() = runTest {
        // Arrange
        every { moneyAccountFeatureToggles.isMoneyAccountsEnabled } returns false
        coEvery {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.PAYMENT_ACCOUNT)
        } returns eligibility(isAvailable = true).right()

        // Act
        val actual = useCase(userWalletId = USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(setOf(TangemPayProduct.PAYMENT_ACCOUNT).right())
        coVerify(exactly = 0) {
            checkTangemPayEligibilityForCustomerUseCase(USER_WALLET_ID, TangemPayProduct.BANKING_ACCOUNT)
        }
    }

    private fun eligibility(isAvailable: Boolean) = TangemPayEligibility(
        isAvailable = isAvailable,
        country = "US",
        region = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}