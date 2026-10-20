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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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