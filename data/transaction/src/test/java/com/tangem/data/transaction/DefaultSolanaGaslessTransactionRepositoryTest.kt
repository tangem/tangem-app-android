package com.tangem.data.transaction

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.grow.datasource.gasless.SolanaGaslessApi
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.tangem.grow.datasource.gasless.models.GaslessServiceResponse
import com.tangem.grow.datasource.gasless.models.solana.SolanaFeePayerResponse
import com.tangem.grow.datasource.gasless.models.solana.SolanaSubmitRequestBody
import com.tangem.grow.datasource.gasless.models.solana.SolanaSubmitResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Base64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSolanaGaslessTransactionRepositoryTest {

    private val api: SolanaGaslessApi = mockk()
    private var nowMillis = 0L
    private lateinit var repository: DefaultSolanaGaslessTransactionRepository
    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
        nowMillis = 0L
        // A fresh instance per test: the fee-payer cache is instance state and must not leak between tests.
        repository = DefaultSolanaGaslessTransactionRepository(
            api = api,
            errorAdapter = Moshi.Builder().build().adapter(GaslessErrorResponse::class.java),
            dispatchers = TestingCoroutineDispatcherProvider(),
            currentTimeMillis = { nowMillis },
        )
    }

    @Test
    fun `GIVEN fee payer response WHEN getFeePayer THEN returns address`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Success(envelope(SolanaFeePayerResponse("FeePayer111")))

        // Act
        val actual = repository.getFeePayer()

        // Assert
        assertThat(actual).isEqualTo("FeePayer111".right())
    }

    @Test
    fun `GIVEN signed bytes WHEN submit THEN sends unwrapped base64 and returns signature`() = runTest {
        // Arrange
        val bytes = ByteArray(200) { it.toByte() }
        coEvery { api.submit(any(), any()) } returns ApiResponse.Success(
            envelope(SolanaSubmitResponse(signature = "sig", status = "broadcast")),
        )

        // Act
        val actual = repository.submit(userWalletId, bytes, lastValidBlockHeight = 77)

        // Assert
        assertThat(actual).isEqualTo("sig".right())
        coVerify {
            api.submit(
                userWalletId = "011",
                body = SolanaSubmitRequestBody(Base64.getEncoder().encodeToString(bytes), 77),
            )
        }
    }

    @Test
    fun `GIVEN cached fee payer WHEN getFeePayer called again THEN api called only once`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Success(envelope(SolanaFeePayerResponse("FeePayer111")))

        // Act
        val first = repository.getFeePayer()
        val second = repository.getFeePayer()

        // Assert
        assertThat(first).isEqualTo("FeePayer111".right())
        assertThat(second).isEqualTo("FeePayer111".right())
        coVerify(exactly = 1) { api.getFeePayer() }
    }

    @Test
    fun `GIVEN fee payer looked up WHEN getFeePayer called within 10 minutes THEN api called only once`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Success(envelope(SolanaFeePayerResponse("FeePayer111")))

        // Act
        repository.getFeePayer()
        nowMillis += 599_999
        val second = repository.getFeePayer()

        // Assert
        assertThat(second).isEqualTo("FeePayer111".right())
        coVerify(exactly = 1) { api.getFeePayer() }
    }

    @Test
    fun `GIVEN fee payer looked up 10 minutes ago WHEN getFeePayer THEN api requested again`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returnsMany listOf(
            ApiResponse.Success(envelope(SolanaFeePayerResponse("FeePayer111"))),
            ApiResponse.Error(
                ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.UNAUTHORIZED, message = null, errorBody = null),
            ) as ApiResponse<GaslessServiceResponse<SolanaFeePayerResponse>>,
        )

        // Act
        repository.getFeePayer()
        nowMillis += 600_000
        val afterWindow = repository.getFeePayer()

        // Assert
        assertThat(afterWindow).isEqualTo(SolanaGaslessError.Unauthorized.left())
        coVerify(exactly = 2) { api.getFeePayer() }
    }

    @Test
    fun `GIVEN failed lookup WHEN getFeePayer called within 60s THEN cached result returned without new api call`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Error(
            ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.UNAUTHORIZED, message = null, errorBody = null),
        ) as ApiResponse<GaslessServiceResponse<SolanaFeePayerResponse>>

        // Act
        val first = repository.getFeePayer()
        nowMillis += 59_000
        val second = repository.getFeePayer()

        // Assert
        assertThat(first).isEqualTo(SolanaGaslessError.Unauthorized.left())
        assertThat(second).isEqualTo(SolanaGaslessError.Unauthorized.left())
        coVerify(exactly = 1) { api.getFeePayer() }
    }

    @Test
    fun `GIVEN failed lookup older than 60s WHEN getFeePayer THEN api requested again`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Error(
            ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.UNAUTHORIZED, message = null, errorBody = null),
        ) as ApiResponse<GaslessServiceResponse<SolanaFeePayerResponse>>

        // Act
        repository.getFeePayer()
        nowMillis += 60_000
        val afterWindow = repository.getFeePayer()

        // Assert
        assertThat(afterWindow).isEqualTo(SolanaGaslessError.Unauthorized.left())
        coVerify(exactly = 2) { api.getFeePayer() }
    }

    @Test
    fun `GIVEN cached fee payer WHEN submit returns Rejected THEN next getFeePayer requests api again`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Success(envelope(SolanaFeePayerResponse("FeePayer111")))
        coEvery { api.submit(any(), any()) } returns ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.UNPROCESSABLE_ENTITY,
                message = null,
                errorBody = err("SOLANA_SIMULATION_FAILED"),
            ),
        ) as ApiResponse<GaslessServiceResponse<SolanaSubmitResponse>>

        // Act
        repository.getFeePayer()
        repository.submit(userWalletId, ByteArray(1), lastValidBlockHeight = 1)
        repository.getFeePayer()

        // Assert
        coVerify(exactly = 2) { api.getFeePayer() }
    }

    @Test
    fun `GIVEN 502 without body WHEN getFeePayer THEN returns ServiceUnavailable`() = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns ApiResponse.Error(
            ApiResponseError.HttpException(code = ApiResponseError.HttpException.Code.BAD_GATEWAY, message = null, errorBody = null),
        ) as ApiResponse<GaslessServiceResponse<SolanaFeePayerResponse>>

        // Act
        val actual = repository.getFeePayer()

        // Assert
        assertThat(actual).isEqualTo(SolanaGaslessError.ServiceUnavailable(code = null).left())
    }

    @ParameterizedTest
    @MethodSource("provideFeePayerTransportFailures")
    fun `GIVEN transport failure WHEN getFeePayer THEN returns ServiceUnavailable`(failure: ApiResponseError) = runTest {
        // Arrange
        coEvery { api.getFeePayer() } returns
            ApiResponse.Error(failure) as ApiResponse<GaslessServiceResponse<SolanaFeePayerResponse>>

        // Act
        val actual = repository.getFeePayer()

        // Assert
        assertThat(actual).isEqualTo(SolanaGaslessError.ServiceUnavailable(code = null).left())
    }

    private fun provideFeePayerTransportFailures() = listOf(
        ApiResponseError.NetworkException(),
        ApiResponseError.TimeoutException(),
    )

    @Test
    fun `GIVEN 409 for already broadcast bytes WHEN submit THEN returns the signature`() = runTest {
        // Arrange
        val conflict = httpError(
            ApiResponseError.HttpException.Code.CONFLICT,
            err("SOLANA_DUPLICATE_SUBMISSION", details = """{"status":"broadcast","signature":"sigY"}"""),
        )
        coEvery { api.submit(any(), any()) } returns
            ApiResponse.Error(conflict) as ApiResponse<GaslessServiceResponse<SolanaSubmitResponse>>

        // Act
        val actual = repository.submit(userWalletId, ByteArray(1), lastValidBlockHeight = 1)

        // Assert
        assertThat(actual).isEqualTo("sigY".right())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun submitErrorMapping(model: ErrorModel) = runTest {
        // Arrange
        coEvery { api.submit(any(), any()) } returns
            ApiResponse.Error(model.error) as ApiResponse<GaslessServiceResponse<SolanaSubmitResponse>>

        // Act
        val actual = repository.submit(userWalletId, ByteArray(1), lastValidBlockHeight = 1)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    data class ErrorModel(val error: ApiResponseError, val expected: SolanaGaslessError)

    private fun provideTestModels() = listOf(
        ErrorModel(httpError(ApiResponseError.HttpException.Code.UNAUTHORIZED), SolanaGaslessError.Unauthorized),
        ErrorModel(httpError(ApiResponseError.HttpException.Code.BAD_REQUEST, err("UNSUPPORTED_CHAIN")), SolanaGaslessError.Disabled),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.GONE, err("SOLANA_BLOCKHASH_EXPIRED")),
            SolanaGaslessError.BlockhashExpired,
        ),
        ErrorModel(
            httpError(
                ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
                err("SOLANA_RATE_LIMIT_EXCEEDED", details = """{"scope":"wallet"}"""),
            ),
            SolanaGaslessError.RateLimited(isPerWallet = true),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.UNPROCESSABLE_ENTITY, err("SOLANA_SIMULATION_FAILED")),
            SolanaGaslessError.Rejected("SOLANA_SIMULATION_FAILED"),
        ),
        ErrorModel(
            httpError(
                ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE,
                err("SOLANA_BROADCAST_UNCERTAIN", details = """{"signature":"sigX"}"""),
            ),
            SolanaGaslessError.MaybeSent(signature = "sigX"),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE, err("EXTERNAL_SERVICE_TIMEOUT")),
            SolanaGaslessError.MaybeSent(signature = null),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE, err("SOLANA_SPEND_CEILING_EXCEEDED")),
            SolanaGaslessError.ServiceUnavailable("SOLANA_SPEND_CEILING_EXCEEDED", isSponsorshipUnavailable = true),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE, err("SPONSOR_INSUFFICIENT_FUNDS")),
            SolanaGaslessError.ServiceUnavailable("SPONSOR_INSUFFICIENT_FUNDS", isSponsorshipUnavailable = true),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE, err("BROADCAST_FAILED")),
            SolanaGaslessError.ServiceUnavailable("BROADCAST_FAILED"),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE, err("CONFIGURATION_MISSING")),
            SolanaGaslessError.ServiceUnavailable("CONFIGURATION_MISSING", isSponsorshipUnavailable = true),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE, err("EXTERNAL_SERVICE_ERROR")),
            SolanaGaslessError.ServiceUnavailable("EXTERNAL_SERVICE_ERROR"),
        ),
        // A gateway 5xx carries no service error code: the backend may still be broadcasting the bytes
        ErrorModel(httpError(ApiResponseError.HttpException.Code.BAD_GATEWAY), SolanaGaslessError.MaybeSent(signature = null)),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.GATEWAY_TIMEOUT, "<html>504 Gateway Time-out</html>"),
            SolanaGaslessError.MaybeSent(signature = null),
        ),
        ErrorModel(
            httpError(ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR, err("SOMETHING_NEW")),
            SolanaGaslessError.MaybeSent(signature = null),
        ),
        ErrorModel(
            httpError(
                ApiResponseError.HttpException.Code.CONFLICT,
                err("SOLANA_DUPLICATE_SUBMISSION", details = """{"status":"in_flight"}"""),
            ),
            SolanaGaslessError.MaybeSent(signature = null),
        ),
        ErrorModel(
            httpError(
                ApiResponseError.HttpException.Code.CONFLICT,
                err("SOLANA_DUPLICATE_SUBMISSION", details = """{"status":"broadcast"}"""),
            ),
            SolanaGaslessError.MaybeSent(signature = null),
        ),
        // A connection reset or a malformed 200 body after submit may still mean the tx was broadcast
        ErrorModel(ApiResponseError.NetworkException(), SolanaGaslessError.MaybeSent(signature = null)),
        ErrorModel(ApiResponseError.TimeoutException(), SolanaGaslessError.MaybeSent(signature = null)),
        ErrorModel(ApiResponseError.UnknownException(IllegalStateException("malformed body")), SolanaGaslessError.MaybeSent(signature = null)),
    )

    private fun httpError(code: ApiResponseError.HttpException.Code, body: String? = null) =
        ApiResponseError.HttpException(code = code, message = null, errorBody = body)

    private fun err(code: String, details: String = "null") =
        """{"error":{"code":"$code","message":"m","details":$details}}"""

    private fun <T> envelope(result: T) = GaslessServiceResponse(result = result, isSuccess = true, timestamp = "t")
}