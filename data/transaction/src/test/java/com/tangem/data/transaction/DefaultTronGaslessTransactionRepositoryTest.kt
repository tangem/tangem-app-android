package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.gasless.TronGaslessApi
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.datasource.api.gasless.models.tron.TronEstimateBreakdown
import com.tangem.datasource.api.gasless.models.tron.TronEstimateResponse
import com.tangem.datasource.api.gasless.models.tron.TronSubmitResponse
import com.tangem.datasource.api.gasless.models.tron.TronTokenDto
import com.tangem.datasource.api.gasless.models.tron.TronTokensResponse
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class DefaultTronGaslessTransactionRepositoryTest {

    private val api: TronGaslessApi = mockk()
    private val repository = DefaultTronGaslessTransactionRepository(api, TestingCoroutineDispatcherProvider())

    private val params = TronGaslessEstimateParams(
        fromAddress = "TFrom",
        toAddress = "TTo",
        tokenContract = "TUSDT",
        amount = "50000000",
        feeTokenContract = "TUSDT",
    )

    @Test
    fun `GIVEN estimate response WHEN estimate THEN maps to domain quote`() = runTest {
        // Arrange
        coEvery { api.estimate(any()) } returns ApiResponse.Success(
            GaslessServiceResponse(
                result = TronEstimateResponse(
                    quoteId = "q_1",
                    feeRecipient = "TFee",
                    compensationToken = "TUSDT",
                    compensationAmount = "2.75",
                    compensationAmountRaw = "2750000",
                    estimate = TronEstimateBreakdown(energy = 78000, bandwidth = 345, trxCost = "8.0722"),
                    expiresAt = "2026-06-03T12:34:56.000Z",
                ),
                isSuccess = true,
                timestamp = "t",
            ),
        )

        // Act
        val quote = repository.estimate(params)

        // Assert
        assertThat(quote.quoteId).isEqualTo("q_1")
        assertThat(quote.feeRecipient).isEqualTo("TFee")
        assertThat(quote.compensationToken).isEqualTo("TUSDT")
        assertThat(quote.compensationAmountRaw).isEqualTo(BigInteger("2750000"))
        assertThat(quote.compensationAmountDecimal).isEqualTo(BigDecimal("2.75"))
        assertThat(quote.energy).isEqualTo(78000L)
        assertThat(quote.bandwidth).isEqualTo(345L)
        assertThat(quote.trxCost).isEqualTo(BigDecimal("8.0722"))
        assertThat(quote.expiresAtEpochMs).isEqualTo(Instant.parse("2026-06-03T12:34:56.000Z").toEpochMilli())
    }

    @Test
    fun `GIVEN unsuccessful estimate WHEN estimate THEN throws`() = runTest {
        // Arrange
        coEvery { api.estimate(any()) } returns ApiResponse.Success(
            GaslessServiceResponse(
                result = TronEstimateResponse(
                    quoteId = "q_1",
                    feeRecipient = "TFee",
                    compensationToken = "TUSDT",
                    compensationAmount = "2.75",
                    compensationAmountRaw = "2750000",
                    estimate = TronEstimateBreakdown(energy = 1, bandwidth = 1, trxCost = "1"),
                    expiresAt = "2026-06-03T12:34:56.000Z",
                ),
                isSuccess = false,
                timestamp = "t",
            ),
        )

        // Act
        val error = runCatching { repository.estimate(params) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN tokens response WHEN getSupportedTokens THEN maps to domain tokens`() = runTest {
        // Arrange
        coEvery { api.getSupportedTokens() } returns ApiResponse.Success(
            GaslessServiceResponse(
                result = TronTokensResponse(
                    tokens = listOf(
                        TronTokenDto(
                            address = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                            symbol = "USDT",
                            decimals = 6,
                            chain = "Tron",
                        ),
                    ),
                ),
                isSuccess = true,
                timestamp = "t",
            ),
        )

        // Act
        val tokens = repository.getSupportedTokens()

        // Assert
        assertThat(tokens).hasSize(1)
        assertThat(tokens[0].contractAddress).isEqualTo("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")
        assertThat(tokens[0].symbol).isEqualTo("USDT")
        assertThat(tokens[0].decimals).isEqualTo(6)
    }

    @Test
    fun `GIVEN tokens already loaded WHEN getSupportedTokens twice THEN api is called once`() = runTest {
        // Arrange
        coEvery { api.getSupportedTokens() } returns tokensResponse(isSuccess = true)

        // Act
        val first = repository.getSupportedTokens()
        val second = repository.getSupportedTokens()

        // Assert
        assertThat(second).isEqualTo(first)
        coVerify(exactly = 1) { api.getSupportedTokens() }
    }

    @Test
    fun `GIVEN unsuccessful tokens response WHEN getSupportedTokens twice THEN api is called again`() = runTest {
        // Arrange
        coEvery { api.getSupportedTokens() } returns tokensResponse(isSuccess = false)

        // Act
        runCatching { repository.getSupportedTokens() }
        runCatching { repository.getSupportedTokens() }

        // Assert
        coVerify(exactly = 2) { api.getSupportedTokens() }
    }

    @Test
    fun `GIVEN submit response WHEN submit THEN maps to domain result`() = runTest {
        // Arrange
        coEvery { api.submit(any()) } returns ApiResponse.Success(
            GaslessServiceResponse(
                result = TronSubmitResponse(
                    compensationTxHash = "hComp",
                    originalTxHash = "hOrig",
                    status = "BROADCAST",
                ),
                isSuccess = true,
                timestamp = "t",
            ),
        )

        // Act
        val result = repository.submit("q_1", "signedComp", "signedOrig")

        // Assert
        assertThat(result.compensationTxHash).isEqualTo("hComp")
        assertThat(result.originalTxHash).isEqualTo("hOrig")
        assertThat(result.status).isEqualTo("BROADCAST")
    }

    private fun tokensResponse(isSuccess: Boolean) = ApiResponse.Success(
        GaslessServiceResponse(
            result = TronTokensResponse(
                tokens = listOf(
                    TronTokenDto(
                        address = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
                        symbol = "USDT",
                        decimals = 6,
                        chain = "Tron",
                    ),
                ),
            ),
            isSuccess = isSuccess,
            timestamp = "t",
        ),
    )
}