package com.tangem.data.swap

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.swap.converter.ExchangeIntentConverter
import com.tangem.domain.core.error.DataError
import com.tangem.domain.express.ExpressErrorResolver
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.ExchangeIntentError
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.express.models.response.ExchangeIntentResponse
import com.tangem.grow.datasource.express.models.response.RateType
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.grow.datasource.express.models.response.ExchangeIntent as ExchangeIntentDto

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultExchangeIntentRepositoryTest {

    private val tangemExpressApi: TangemExpressApi = mockk()
    private val exchangeIntentConverter: ExchangeIntentConverter = mockk()
    private val expressErrorResolver: ExpressErrorResolver = mockk()
    private val repository = DefaultExchangeIntentRepository(
        tangemExpressApi = tangemExpressApi,
        exchangeIntentConverter = exchangeIntentConverter,
        expressErrorResolver = expressErrorResolver,
    )

    private val walletId = UserWalletId("011")
    private val response = ExchangeIntentResponse(intent = createIntentDto(), transactions = emptyList())

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemExpressApi, exchangeIntentConverter, expressErrorResolver)
    }

    @Nested
    inner class Success {

        @Test
        fun `GIVEN express answers WHEN getExchangeIntent THEN the converted exchange intent is returned`() = runTest {
            // Arrange
            val exchange = createExchangeIntent()
            stubApi(ApiResponse.Success(response))
            every { exchangeIntentConverter.convert(response) } returns Either.Right(exchange)

            // Act
            val actual = repository.getExchangeIntent(walletId, INTENT_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(exchange))
        }

        @Test
        fun `GIVEN wallet and intent ids WHEN getExchangeIntent THEN express is asked as that wallet for that intent`() =
            runTest {
                // Arrange
                stubApi(ApiResponse.Success(response))
                every { exchangeIntentConverter.convert(response) } returns Either.Right(createExchangeIntent())

                // Act
                repository.getExchangeIntent(walletId, INTENT_ID)

                // Assert
                coVerify(exactly = 1) {
                    tangemExpressApi.getExchangeIntent(userWalletId = walletId.stringValue, intentId = INTENT_ID)
                }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Failure {

        @Test
        fun `GIVEN intent of another wallet WHEN getExchangeIntent THEN wrong wallet is reported`() = runTest {
            // Arrange
            val failure = httpError(ApiResponseError.HttpException.Code.FORBIDDEN)
            stubError(failure)
            every { expressErrorResolver.resolve(failure) } returns ExpressError.IntentOfAnotherWalletError(code = 2610)

            // Act
            val actual = repository.getExchangeIntent(walletId, INTENT_ID)

            // Assert
            val expected = DataError.UserWalletError.WrongUserWallet("Intent $INTENT_ID belongs to another wallet")
            assertThat(actual).isEqualTo(Either.Left(expected))
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN failure that is not another wallet's intent WHEN getExchangeIntent THEN it is returned as it is`(
            model: RawFailureModel,
        ) = runTest {
            // Arrange
            stubError(model.failure)
            every { expressErrorResolver.resolve(model.failure) } returns model.resolved

            // Act
            val actual = repository.getExchangeIntent(walletId, INTENT_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(model.failure))
        }

        @Suppress("unused")
        private fun provideTestModels() = listOf(
            RawFailureModel(
                failure = httpError(ApiResponseError.HttpException.Code.NOT_FOUND),
                resolved = ExpressError.DataError(code = 2600, description = "Core error"),
            ),
            RawFailureModel(
                failure = httpError(ApiResponseError.HttpException.Code.FORBIDDEN),
                resolved = ExpressError.UnknownError,
            ),
            RawFailureModel(
                failure = ApiResponseError.NetworkException(),
                resolved = ExpressError.UnknownError,
            ),
        )

        @Test
        fun `GIVEN converter rejecting the response WHEN getExchangeIntent THEN its error is returned`() = runTest {
            // Arrange
            val error = ExchangeIntentError.AmbiguousAmountSide(INTENT_ID)
            stubApi(ApiResponse.Success(response))
            every { exchangeIntentConverter.convert(response) } returns Either.Left(error)

            // Act
            val actual = repository.getExchangeIntent(walletId, INTENT_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(error))
        }
    }

    data class RawFailureModel(val failure: ApiResponseError, val resolved: ExpressError)

    private fun stubApi(response: ApiResponse<ExchangeIntentResponse>) {
        coEvery { tangemExpressApi.getExchangeIntent(any(), any()) } returns response
    }

    @Suppress("UNCHECKED_CAST")
    private fun stubError(cause: ApiResponseError) {
        stubApi(ApiResponse.Error(cause) as ApiResponse<ExchangeIntentResponse>)
    }

    private fun httpError(code: ApiResponseError.HttpException.Code): ApiResponseError {
        return ApiResponseError.HttpException(code = code, message = null, errorBody = null)
    }

    private fun createIntentDto() = ExchangeIntentDto(
        intentId = INTENT_ID,
        clientId = "claude-code",
        sessionId = "session-1",
        walletId = "wallet-1",
        provider = "changelly",
        rateType = RateType.FLOAT,
        fromNetwork = "ethereum",
        fromContractAddress = "0",
        fromDecimals = 18,
        fromAmount = "1000000000000000000",
        fromAddress = "0xFrom",
        toNetwork = "tron",
        toContractAddress = "0",
        toDecimals = 6,
        toAmount = null,
        payoutAddress = "TPayout",
        toExtraId = null,
    )

    private fun createExchangeIntent() = ExchangeIntent(
        id = INTENT_ID,
        walletId = "wallet-1",
        clientId = "claude-code",
        sessionId = "session-1",
        isHandled = false,
        amountType = SwapAmountType.From,
        amount = "1000000000000000000",
        from = ExchangeIntent.Asset("ethereum", contractAddress = "0", decimals = 18, address = "0xFrom"),
        to = ExchangeIntent.Asset("tron", contractAddress = "0", decimals = 6, address = "TPayout"),
        providerId = "changelly",
        rateType = ExpressRateType.Float,
        toExtraId = null,
    )

    private companion object {
        const val INTENT_ID = "fb9488fc-e44c-435c-b074-144b5a288c42"
    }
}