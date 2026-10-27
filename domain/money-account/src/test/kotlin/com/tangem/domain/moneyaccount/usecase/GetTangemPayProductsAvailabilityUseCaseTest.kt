package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetTangemPayProductsAvailabilityForCustomerUseCase
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetTangemPayProductsAvailabilityUseCaseTest {

    private val getTangemPayProductsAvailabilityForCustomerUseCase:
        GetTangemPayProductsAvailabilityForCustomerUseCase = mockk()
    private val isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase = mockk()
    private val userWalletId: UserWalletId = mockk()

    private val useCase = GetTangemPayProductsAvailabilityUseCase(
        getTangemPayProductsAvailabilityForCustomerUseCase = getTangemPayProductsAvailabilityForCustomerUseCase,
        isMoneyAccountPathOccupiedUseCase = isMoneyAccountPathOccupiedUseCase,
    )

    @BeforeEach
    fun setup() {
        clearMocks(getTangemPayProductsAvailabilityForCustomerUseCase, isMoneyAccountPathOccupiedUseCase)
    }

    @Test
    fun `GIVEN money account path occupied WHEN invoke THEN banking account is excluded`() = runTest {
        // Arrange
        coEvery { getTangemPayProductsAvailabilityForCustomerUseCase(userWalletId) } returns
            setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT).right()
        coEvery { isMoneyAccountPathOccupiedUseCase(userWalletId) } returns true

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(setOf(TangemPayProduct.PAYMENT_ACCOUNT).right())
    }

    @Test
    fun `GIVEN money account path free WHEN invoke THEN all products returned`() = runTest {
        // Arrange
        val products = setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT)
        coEvery { getTangemPayProductsAvailabilityForCustomerUseCase(userWalletId) } returns products.right()
        coEvery { isMoneyAccountPathOccupiedUseCase(userWalletId) } returns false

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(products.right())
    }

    @Test
    fun `GIVEN banking account not available WHEN invoke THEN path is not checked`() = runTest {
        // Arrange
        val products = setOf(TangemPayProduct.PAYMENT_ACCOUNT)
        coEvery { getTangemPayProductsAvailabilityForCustomerUseCase(userWalletId) } returns products.right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(products.right())
        coVerify(exactly = 0) { isMoneyAccountPathOccupiedUseCase(any()) }
    }

    @Test
    fun `GIVEN availability request fails WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val error: VisaApiError = mockk()
        coEvery { getTangemPayProductsAvailabilityForCustomerUseCase(userWalletId) } returns error.left()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isEqualTo(error.left())
        coVerify(exactly = 0) { isMoneyAccountPathOccupiedUseCase(any()) }
    }
}