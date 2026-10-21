package com.tangem.data.transaction

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.transaction.api.TransactionIntentApi
import com.tangem.data.transaction.api.models.ConfirmIntentTransactionRequest
import com.tangem.data.transaction.api.models.SendIntentResponse
import com.tangem.data.transaction.convertes.SendIntentConverter
import com.tangem.data.transaction.convertes.SendIntentErrorConverter
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.domain.transaction.models.SendIntent
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSendIntentRepositoryTest {

    private val transactionIntentApi: TransactionIntentApi = mockk()
    private val repository = DefaultSendIntentRepository(
        transactionIntentApi = transactionIntentApi,
        sendIntentConverter = SendIntentConverter(),
        sendIntentErrorConverter = SendIntentErrorConverter(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(transactionIntentApi)
    }

    @Test
    fun `GIVEN the contract example WHEN getSendIntent THEN it is read into a send intent`() = runTest {
        // Arrange
        val response = NetworkJsonFactory.create().decodeFromString<SendIntentResponse>(CONTRACT_EXAMPLE)
        coEvery { transactionIntentApi.getSendIntent(INTENT_ID) } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getSendIntent(INTENT_ID)

        // Assert
        val expected = SendIntent(
            id = INTENT_ID,
            clientId = "string",
            sessionId = "string",
            isHandled = false,
            networkId = "polygon-pos",
            tokenId = "tether",
            fromAddress = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
            toAddress = "0x1111111111111111111111111111111111111111",
            amount = BigDecimal("12.5"),
            memo = "12345",
            comment = "Some comment",
        )
        assertThat(actual).isEqualTo(Either.Right(expected))
        coVerify(exactly = 1) { transactionIntentApi.getSendIntent(INTENT_ID) }
    }

    @Test
    fun `GIVEN response without the optional fields WHEN decoded THEN they are absent`() {
        // Act
        val actual = NetworkJsonFactory.create().decodeFromString<SendIntentResponse>(REQUIRED_FIELDS_ONLY)

        // Assert
        assertThat(listOf(actual.memo, actual.comment, actual.txHash, actual.clientId))
            .containsExactly(null, null, null, null)
    }

    @Test
    fun `GIVEN service answers intent not found WHEN getSendIntent THEN not found is reported`() = runTest {
        // Arrange
        val failure = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.NOT_FOUND,
            message = null,
            errorBody = """{"status":404,"code":"INTENT_NOT_FOUND"}""",
        )
        stubError(failure)

        // Act
        val actual = repository.getSendIntent(INTENT_ID)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(SendIntentError.NotFound(INTENT_ID)))
    }

    @Test
    fun `GIVEN service fails with 5xx WHEN getSendIntent THEN server error with that code is reported`() = runTest {
        // Arrange
        val failure = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE,
            message = null,
            errorBody = null,
        )
        stubError(failure)

        // Act
        val actual = repository.getSendIntent(INTENT_ID)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(SendIntentError.ServerError(INTENT_ID, httpCode = 503)))
    }

    @Test
    fun `GIVEN service fails with 5xx WHEN completeSendIntent THEN the raw failure is returned to be retried`() =
        runTest {
            // Arrange
            val failure = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
                message = null,
                errorBody = null,
            )
            stubConfirm(ApiResponse.Error(failure) as ApiResponse<Unit>)

            // Act
            val actual = repository.completeSendIntent(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(failure))
        }

    @Test
    fun `GIVEN no network WHEN getSendIntent THEN the raw failure is returned`() = runTest {
        // Arrange
        val failure = ApiResponseError.NetworkException()
        stubError(failure)

        // Act
        val actual = repository.getSendIntent(INTENT_ID)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(failure))
    }

    @Test
    fun `GIVEN service accepts the hash WHEN completeSendIntent THEN it succeeds`() = runTest {
        // Arrange
        stubConfirm(ApiResponse.Success(Unit, code = ApiResponseError.HttpException.Code.NO_CONTENT))

        // Act
        val actual = repository.completeSendIntent(INTENT_ID, TX_HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 1) {
            transactionIntentApi.confirmIntentTransaction(INTENT_ID, ConfirmIntentTransactionRequest(txHash = TX_HASH))
        }
    }

    @Test
    fun `GIVEN intent carries another hash WHEN completeSendIntent THEN already completed is reported`() = runTest {
        // Arrange
        val failure = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.CONFLICT,
            message = null,
            errorBody = """{"status":409,"code":"INTENT_ALREADY_COMPLETED"}""",
        )
        stubConfirm(ApiResponse.Error(failure) as ApiResponse<Unit>)

        // Act
        val actual = repository.completeSendIntent(INTENT_ID, TX_HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(SendIntentError.AlreadyCompleted(INTENT_ID)))
    }

    @Test
    fun `GIVEN service answers intent not found WHEN completeSendIntent THEN not found is reported`() = runTest {
        // Arrange
        val failure = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.NOT_FOUND,
            message = null,
            errorBody = """{"status":404,"code":"INTENT_NOT_FOUND"}""",
        )
        stubConfirm(ApiResponse.Error(failure) as ApiResponse<Unit>)

        // Act
        val actual = repository.completeSendIntent(INTENT_ID, TX_HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(SendIntentError.NotFound(INTENT_ID)))
    }

    @Test
    fun `GIVEN request times out WHEN completeSendIntent THEN the raw failure is returned`() = runTest {
        // Arrange
        val failure = ApiResponseError.TimeoutException()
        stubConfirm(ApiResponse.Error(failure) as ApiResponse<Unit>)

        // Act
        val actual = repository.completeSendIntent(INTENT_ID, TX_HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(failure))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReportHash {

        @Test
        fun `GIVEN service accepts the hash WHEN reportSendIntentHash THEN it is sent once without a pause`() = runTest {
            // Arrange
            stubConfirm(ApiResponse.Success(Unit, code = ApiResponseError.HttpException.Code.NO_CONTENT))

            // Act
            val actual = repository.reportSendIntentHash(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            assertThat(testScheduler.currentTime).isEqualTo(0L)
            coVerify(exactly = 1) { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) }
        }

        @Test
        fun `GIVEN no network at first WHEN reportSendIntentHash THEN it is sent again after a pause`() = runTest {
            // Arrange
            coEvery { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) } returnsMany listOf(
                failure(ApiResponseError.NetworkException()),
                ApiResponse.Success(Unit, code = ApiResponseError.HttpException.Code.NO_CONTENT),
            )

            // Act
            val actual = repository.reportSendIntentHash(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            assertThat(testScheduler.currentTime).isEqualTo(2_000L)
            coVerify(exactly = 2) { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) }
        }

        @Test
        fun `GIVEN service never reachable WHEN reportSendIntentHash THEN it gives up with the last failure`() = runTest {
            // Arrange
            val last = ApiResponseError.TimeoutException()
            coEvery { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) } returnsMany listOf(
                failure(ApiResponseError.NetworkException()),
                failure(ApiResponseError.NetworkException()),
                failure(ApiResponseError.NetworkException()),
                failure(last),
            )

            // Act
            val actual = repository.reportSendIntentHash(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(last))
            assertThat(testScheduler.currentTime).isEqualTo(22_000L)
            coVerify(exactly = 4) { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN failure that may pass WHEN reportSendIntentHash THEN it is tried again`(
            code: ApiResponseError.HttpException.Code,
        ) = runTest {
            // Arrange
            coEvery { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) } returnsMany listOf(
                failure(ApiResponseError.HttpException(code = code, message = null, errorBody = null)),
                ApiResponse.Success(Unit, code = ApiResponseError.HttpException.Code.NO_CONTENT),
            )

            // Act
            val actual = repository.reportSendIntentHash(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            coVerify(exactly = 2) { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) }
        }

        @Suppress("unused")
        private fun provideTestModels() = listOf(
            ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
            ApiResponseError.HttpException.Code.BAD_GATEWAY,
            ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE,
            ApiResponseError.HttpException.Code.UNAUTHORIZED,
            ApiResponseError.HttpException.Code.REQUEST_TIMEOUT,
            ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
        )

        @Test
        fun `GIVEN service refuses the hash for good WHEN reportSendIntentHash THEN it is not tried again`() = runTest {
            // Arrange
            val badRequest = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.BAD_REQUEST,
                message = null,
                errorBody = null,
            )
            stubConfirm(failure(badRequest))

            // Act
            val actual = repository.reportSendIntentHash(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(badRequest))
            assertThat(testScheduler.currentTime).isEqualTo(0L)
            coVerify(exactly = 1) { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) }
        }

        @Test
        fun `GIVEN intent already carries another hash WHEN reportSendIntentHash THEN it is not tried again`() = runTest {
            // Arrange
            val conflict = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.CONFLICT,
                message = null,
                errorBody = """{"status":409,"code":"INTENT_ALREADY_COMPLETED"}""",
            )
            stubConfirm(failure(conflict))

            // Act
            val actual = repository.reportSendIntentHash(INTENT_ID, TX_HASH)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(SendIntentError.AlreadyCompleted(INTENT_ID)))
            coVerify(exactly = 1) { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) }
        }

        @Suppress("UNCHECKED_CAST")
        private fun failure(cause: ApiResponseError): ApiResponse<Unit> = ApiResponse.Error(cause) as ApiResponse<Unit>
    }

    private fun stubConfirm(response: ApiResponse<Unit>) {
        coEvery { transactionIntentApi.confirmIntentTransaction(INTENT_ID, any()) } returns response
    }

    private fun stubError(cause: ApiResponseError) {
        coEvery {
            transactionIntentApi.getSendIntent(INTENT_ID)
        } returns ApiResponse.Error(cause) as ApiResponse<SendIntentResponse>
    }

    private companion object {
        const val INTENT_ID = "3f7c2a1e-9b4d-4c6a-8e21-5d0f7a9b1c33"
        const val TX_HASH = "0x5555555555555555555555555555555555555555555555555555555555555555"

        /** The response example of `GET /api/transactions/v1/intents/{intentId}` in the Notion contract. */
        val CONTRACT_EXAMPLE = """
            {
              "intentId": "$INTENT_ID",
              "type": "SEND",
              "status": "CREATED",
              "networkId": "polygon-pos",
              "fromAddress": "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
              "toAddress": "0x1111111111111111111111111111111111111111",
              "tokenId": "tether",
              "amount": "12.5",
              "memo": "12345",
              "comment": "Some comment",
              "createdAt": "2026-09-17T13:05:12Z",
              "clientId": "string",
              "txHash": null,
              "sessionId": "string"
            }
        """.trimIndent()

        val REQUIRED_FIELDS_ONLY = """
            {
              "intentId": "$INTENT_ID",
              "type": "SEND",
              "status": "CREATED",
              "networkId": "polygon-pos",
              "fromAddress": "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
              "toAddress": "0x1111111111111111111111111111111111111111",
              "tokenId": "tether",
              "amount": "12.5",
              "createdAt": "2026-09-17T13:05:12Z",
              "sessionId": "string"
            }
        """.trimIndent()
    }
}