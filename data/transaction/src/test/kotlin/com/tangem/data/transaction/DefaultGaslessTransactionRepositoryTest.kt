package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.gasless.GaslessTxServiceApi
import com.tangem.datasource.api.gasless.GaslessTxServiceApiV2
import com.tangem.datasource.api.gasless.models.GaslessFeeRecipient
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultGaslessTransactionRepositoryTest {

    private val gaslessTxServiceApi: GaslessTxServiceApi = mockk()
    private val gaslessTxServiceApiV2: GaslessTxServiceApiV2 = mockk()
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(gaslessTxServiceApi, gaslessTxServiceApiV2, responseCryptoCurrenciesFactory)
    }

    private fun createRepository() = DefaultGaslessTransactionRepository(
        gaslessTxServiceApi = gaslessTxServiceApi,
        gaslessTxServiceApiV2 = gaslessTxServiceApiV2,
        isGaslessV2Enabled = true,
        coroutineDispatcherProvider = TestingCoroutineDispatcherProvider(),
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
    )

    private fun stubFeeRecipientSuccess(address: String) {
        coEvery { gaslessTxServiceApi.getFeeRecipient() } returns ApiResponse.Success(
            data = GaslessServiceResponse(
                result = GaslessFeeRecipient(address = address),
                isSuccess = true,
                timestamp = "2026-06-11T00:00:00.000Z",
            ),
        )
    }

    private fun stubFeeRecipientFailure() {
        @Suppress("UNCHECKED_CAST")
        val error = ApiResponse.Error(cause = ApiResponseError.NetworkException())
            as ApiResponse<GaslessServiceResponse<GaslessFeeRecipient>>
        coEvery { gaslessTxServiceApi.getFeeRecipient() } returns error
    }

    @Test
    fun `GIVEN backend returns recipient WHEN getGaslessFeeAddresses THEN hardcoded plus backend address`() = runTest {
        // Arrange
        stubFeeRecipientSuccess(BACKEND_ADDRESS)
        val repository = createRepository()

        // Act
        val actual = repository.getGaslessFeeAddresses()

        // Assert
        assertThat(actual).containsExactly(HARDCODED_ADDRESS_1, HARDCODED_ADDRESS_2, BACKEND_ADDRESS)
    }

    @Test
    fun `GIVEN backend fails WHEN getGaslessFeeAddresses THEN hardcoded addresses only`() = runTest {
        // Arrange
        stubFeeRecipientFailure()
        val repository = createRepository()

        // Act
        val actual = repository.getGaslessFeeAddresses()

        // Assert
        assertThat(actual).containsExactly(HARDCODED_ADDRESS_1, HARDCODED_ADDRESS_2)
    }

    @Test
    fun `GIVEN backend fails then recovers WHEN called twice THEN second call includes backend address`() = runTest {
        // Arrange
        stubFeeRecipientFailure()
        val repository = createRepository()
        val firstResult = repository.getGaslessFeeAddresses()
        stubFeeRecipientSuccess(BACKEND_ADDRESS)

        // Act
        val secondResult = repository.getGaslessFeeAddresses()

        // Assert
        assertThat(firstResult).containsExactly(HARDCODED_ADDRESS_1, HARDCODED_ADDRESS_2)
        assertThat(secondResult).containsExactly(HARDCODED_ADDRESS_1, HARDCODED_ADDRESS_2, BACKEND_ADDRESS)
    }

    @Test
    fun `GIVEN backend succeeds WHEN called twice THEN fee recipient requested once`() = runTest {
        // Arrange
        stubFeeRecipientSuccess(BACKEND_ADDRESS)
        val repository = createRepository()

        // Act
        repository.getGaslessFeeAddresses()
        repository.getGaslessFeeAddresses()

        // Assert
        coVerify(exactly = 1) { gaslessTxServiceApi.getFeeRecipient() }
    }

    private companion object {
        const val HARDCODED_ADDRESS_1 = "0xFc719364BcCdc92D055d8C3164eF1ab4f5A9182c"
        const val HARDCODED_ADDRESS_2 = "0xAf722F46145fbb106379d506ED3a5B96f110c8E5"
        const val BACKEND_ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}