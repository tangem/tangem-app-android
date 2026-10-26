package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetMoneyAccountFeeTableUseCaseTest {

    private val repository: MoneyAccountRepository = mockk()

    private lateinit var useCase: GetMoneyAccountFeeTableUseCase

    @BeforeEach
    fun setUp() {
        clearMocks(repository)
        useCase = GetMoneyAccountFeeTableUseCase(moneyAccountRepository = repository)
    }

    @Test
    fun `GIVEN payment methods with fees WHEN invoke THEN returns them`() = runTest {
        // Arrange
        val paymentMethods = listOf(
            paymentMethod(
                receiveFee = fee(type = "ACH_ONRAMP", amount = BigDecimal("1.00")),
                sendFee = fee(type = "ACH_OFFRAMP", amount = BigDecimal("1.50")),
            ),
        )
        coEvery { repository.getPaymentMethods() } returns paymentMethods.right()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(paymentMethods.right())
    }

    @Test
    fun `GIVEN payment methods unavailable WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery { repository.getPaymentMethods() } returns VisaApiError.ServerUnavailable.left()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
    }

    @Test
    fun `GIVEN payment methods loaded WHEN invoke again THEN cached result returned`() = runTest {
        // Arrange
        val paymentMethods = listOf(paymentMethod())
        coEvery { repository.getPaymentMethods() } returns paymentMethods.right()
        useCase()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(paymentMethods.right())
        coVerify(exactly = 1) { repository.getPaymentMethods() }
    }

    @Test
    fun `GIVEN previous request failed WHEN invoke again THEN payment methods requested again`() = runTest {
        // Arrange
        val paymentMethods = listOf(paymentMethod())
        coEvery { repository.getPaymentMethods() } returnsMany listOf(
            VisaApiError.ServerUnavailable.left(),
            paymentMethods.right(),
        )
        useCase()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(paymentMethods.right())
        coVerify(exactly = 2) { repository.getPaymentMethods() }
    }

    private fun paymentMethod(
        receiveFee: MoneyAccountPaymentMethod.Fee? = null,
        sendFee: MoneyAccountPaymentMethod.Fee? = null,
    ) = MoneyAccountPaymentMethod(
        id = "ACH",
        name = "ACH",
        countryCode = "US",
        iconUrl = "https://tangem.com/flags/us.png",
        receiveFee = receiveFee,
        sendFee = sendFee,
    )

    private fun fee(type: String, amount: BigDecimal) = MoneyAccountPaymentMethod.Fee(
        type = type,
        amount = amount,
        currency = "USD",
    )
}