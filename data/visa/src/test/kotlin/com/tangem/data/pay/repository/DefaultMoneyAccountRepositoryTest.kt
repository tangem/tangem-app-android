package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.PaymentMethodsResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@Suppress("UNCHECKED_CAST")
internal class DefaultMoneyAccountRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestPerformer: TangemPayRequestPerformer = mockk()

    private val repository = DefaultMoneyAccountRepository(
        tangemPayApi = tangemPayApi,
        requestPerformer = requestPerformer,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestPerformer)

        coEvery { requestPerformer.performWithStaticToken<Any>(requestBlock = any()) } coAnswers {
            val block = firstArg<suspend () -> ApiResponse<Any>>()
            when (val response = block()) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
    }

    @Test
    fun `GIVEN payment methods WHEN getPaymentMethods THEN converted without a token`() = runTest {
        // Arrange
        val response = PaymentMethodsResponse(
            result = listOf(
                PaymentMethodsResponse.PaymentMethod(
                    id = "ACH",
                    name = "ACH",
                    countryCode = "US",
                    iconUrl = "https://tangem.com/flags/us.png",
                    fees = PaymentMethodsResponse.Fees(
                        receive = feeResponse(type = "ACH_ONRAMP", amount = BigDecimal("1.00")),
                        send = feeResponse(type = "ACH_OFFRAMP", amount = BigDecimal("1.50")),
                    ),
                ),
                PaymentMethodsResponse.PaymentMethod(
                    id = null,
                    name = "Broken",
                    countryCode = null,
                    iconUrl = null,
                    fees = null,
                ),
            ),
        )
        coEvery { tangemPayApi.getPaymentMethods() } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getPaymentMethods()

        // Assert
        val expected = listOf(
            MoneyAccountPaymentMethod(
                id = "ACH",
                name = "ACH",
                countryCode = "US",
                iconUrl = "https://tangem.com/flags/us.png",
                receiveFee = MoneyAccountPaymentMethod.Fee(
                    type = "ACH_ONRAMP",
                    amount = BigDecimal("1.00"),
                    currency = "USD",
                ),
                sendFee = MoneyAccountPaymentMethod.Fee(
                    type = "ACH_OFFRAMP",
                    amount = BigDecimal("1.50"),
                    currency = "USD",
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN missing send fee and broken receive fee WHEN getPaymentMethods THEN both fees are null`() = runTest {
        // Arrange
        val response = PaymentMethodsResponse(
            result = listOf(
                PaymentMethodsResponse.PaymentMethod(
                    id = "FEDWIRE",
                    name = "FedWire",
                    countryCode = "US",
                    iconUrl = null,
                    fees = PaymentMethodsResponse.Fees(
                        receive = feeResponse(type = "FEDWIRE_ONRAMP", amount = null),
                        send = null,
                    ),
                ),
            ),
        )
        coEvery { tangemPayApi.getPaymentMethods() } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getPaymentMethods()

        // Assert
        val expected = listOf(
            MoneyAccountPaymentMethod(
                id = "FEDWIRE",
                name = "FedWire",
                countryCode = "US",
                iconUrl = null,
                receiveFee = null,
                sendFee = null,
            ),
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN failing request WHEN getPaymentMethods THEN api error`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getPaymentMethods() } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<PaymentMethodsResponse>

        // Act
        val actual = repository.getPaymentMethods()

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun feeResponse(type: String, amount: BigDecimal?) = PaymentMethodsResponse.Fee(
        type = type,
        calculationType = "FIXED",
        amount = amount,
        currency = "USD",
    )
}