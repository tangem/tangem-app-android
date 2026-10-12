package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.tangem.grow.datasource.gasless.models.GaslessServiceResponse
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateBreakdown
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateResponse
import com.tangem.grow.datasource.gasless.models.tron.TronSubmitResponse
import com.tangem.grow.datasource.gasless.models.tron.TronTokenDto
import com.tangem.grow.datasource.gasless.models.tron.TronTokensResponse
import com.tangem.domain.transaction.error.GaslessServiceException
import com.tangem.domain.transaction.error.TronActivationRefusal
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class DefaultTronGaslessTransactionRepositoryTest {

    private val api: TronGaslessApi = mockk()
    private val repository = DefaultTronGaslessTransactionRepository(
        api = api,
        errorAdapter = Moshi.Builder().build().adapter(GaslessErrorResponse::class.java),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")
    private val params = TronGaslessEstimateParams(
        userWalletId = userWalletId,
        fromAddress = "TFrom",
        toAddress = "TTo",
        tokenContract = "TUSDT",
        amount = BigInteger("50000000"),
        feeTokenContract = "TUSDT",
    )

    @Test
    fun `GIVEN estimate response WHEN estimate THEN maps to domain quote`() = runTest {
        // Arrange
        coEvery { api.estimate(any(), any()) } returns ApiResponse.Success(
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
        assertThat(quote.quotedAmountRaw).isEqualTo(BigInteger("50000000"))
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
        coEvery { api.estimate(any(), any()) } returns ApiResponse.Success(
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
        assertThat(error).isInstanceOf(GaslessServiceException::class.java)
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
        coEvery { api.submit(any(), any()) } returns ApiResponse.Success(
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
        val result = repository.submit(userWalletId, "q_1", "signedComp", "signedOrig")

        // Assert
        assertThat(result.compensationTxHash).isEqualTo("hComp")
        assertThat(result.originalTxHash).isEqualTo("hOrig")
        assertThat(result.status).isEqualTo("BROADCAST")
        coVerify { api.submit(userWalletId.stringValue, any()) }
    }

    @Test
    fun `GIVEN estimate WHEN estimate THEN wallet id is sent to the service`() = runTest {
        // Arrange
        coEvery { api.estimate(any(), any()) } returns httpError(
            code = ApiResponseError.HttpException.Code.BAD_REQUEST,
            errorBody = null,
        )

        // Act
        runCatching { repository.estimate(params) }

        // Assert
        coVerify { api.estimate(userWalletId.stringValue, any()) }
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN service error body WHEN estimate THEN refusal and message are carried`(model: ErrorBodyModel) = runTest {
        // Arrange
        coEvery { api.estimate(any(), any()) } returns httpError(
            code = ApiResponseError.HttpException.Code.UNPROCESSABLE_ENTITY,
            errorBody = model.errorBody,
        )

        // Act
        val error = runCatching { repository.estimate(params) }.exceptionOrNull() as GaslessServiceException

        // Assert
        assertThat(error.httpCode).isEqualTo(422)
        assertThat(error.message).isEqualTo(model.expectedMessage)
        assertThat(error.tronActivationRefusal).isEqualTo(model.expectedRefusal)
    }

    @Test
    fun `GIVEN non-JSON error body WHEN estimate THEN http error without a code`() = runTest {
        // Arrange
        coEvery { api.estimate(any(), any()) } returns httpError(
            code = ApiResponseError.HttpException.Code.BAD_GATEWAY,
            errorBody = "<html>Bad gateway</html>",
        )

        // Act
        val error = runCatching { repository.estimate(params) }.exceptionOrNull() as GaslessServiceException

        // Assert
        assertThat(error.httpCode).isEqualTo(502)
        assertThat(error.message).isEqualTo("Bad Gateway")
        assertThat(error.tronActivationRefusal).isNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun httpError(code: ApiResponseError.HttpException.Code, errorBody: String?) = ApiResponse.Error(
        ApiResponseError.HttpException(code = code, message = null, errorBody = errorBody),
    ) as ApiResponse<GaslessServiceResponse<TronEstimateResponse>>

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

    companion object {
        data class ErrorBodyModel(
            val case: String,
            val errorBody: String,
            val expectedMessage: String,
            val expectedRefusal: TronActivationRefusal?,
        ) {
            override fun toString(): String = case
        }

        @JvmStatic
        fun provideTestModels() = listOf(
            ErrorBodyModel(
                case = "balance too low, minimum as a string",
                errorBody = errorBody(code = "TRON_ACTIVATION_BALANCE_TOO_LOW", details = """{"minimum":"5000000"}"""),
                expectedMessage = "refused",
                expectedRefusal = TronActivationRefusal.BalanceTooLow(minimumAmountRaw = BigInteger("5000000")),
            ),
            ErrorBodyModel(
                case = "balance too low, minimum as a number",
                errorBody = errorBody(code = "TRON_ACTIVATION_BALANCE_TOO_LOW", details = """{"minimum":5000000}"""),
                expectedMessage = "refused",
                expectedRefusal = TronActivationRefusal.BalanceTooLow(minimumAmountRaw = BigInteger("5000000")),
            ),
            ErrorBodyModel(
                case = "balance too low without a minimum",
                errorBody = errorBody(code = "TRON_ACTIVATION_BALANCE_TOO_LOW", details = "{}"),
                expectedMessage = "refused",
                expectedRefusal = null,
            ),
            ErrorBodyModel(
                case = "rate limited per wallet",
                errorBody = errorBody(code = "TRON_ACTIVATION_RATE_LIMITED", details = """{"scope":"wallet"}"""),
                expectedMessage = "refused",
                expectedRefusal = TronActivationRefusal.RateLimited(isPerWallet = true),
            ),
            ErrorBodyModel(
                case = "rate limited service-wide",
                errorBody = errorBody(code = "TRON_ACTIVATION_RATE_LIMITED", details = """{"scope":"global"}"""),
                expectedMessage = "refused",
                expectedRefusal = TronActivationRefusal.RateLimited(isPerWallet = false),
            ),
            ErrorBodyModel(
                case = "sponsorship off",
                errorBody = errorBody(code = "TRON_ACCOUNT_NOT_ACTIVATED", details = "{}"),
                expectedMessage = "refused",
                expectedRefusal = null,
            ),
        )

        private fun errorBody(code: String, details: String) =
            """{"success":false,"error":{"code":"$code","message":"refused","details":$details},"statusCode":422}"""
    }
}