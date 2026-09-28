package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.error.UniversalError
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayCardProvisioningData
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.config.TangemPay
import com.tangem.spend.datasource.pay.TangemPayProvisioningApi
import com.tangem.spend.datasource.pay.models.response.CardProcessorDetailsResponse
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DefaultTangemPayCardDetailsRepositoryTest {

    private val tangemPayProvisioningApi: TangemPayProvisioningApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val apiConfigsManager: ApiConfigsManager = mockk()

    private val userWalletId = UserWalletId("011")

    private val repository = DefaultTangemPayCardDetailsRepository(
        tangemPayApi = mockk(),
        tangemPayProvisioningApi = tangemPayProvisioningApi,
        requestHelper = requestHelper,
        visaLibLoader = mockk(),
        apiConfigsManager = apiConfigsManager,
        rainCryptoUtil = mockk(),
        storage = mockk(),
        cardFrozenStateStore = mockk(),
        errorConverter = mockk(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayProvisioningApi, requestHelper, apiConfigsManager)
        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.ServerUnavailable.left()
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetProvisioningData {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN processor details response WHEN getProvisioningData THEN expected result returned`(
            model: GetProvisioningDataModel,
        ) = runTest {
            // Arrange
            coEvery {
                tangemPayProvisioningApi.getCardProcessorDetails(authHeader = AUTH_HEADER, cardId = CARD_ID)
            } returns model.response

            // Act
            val actual = repository.getProvisioningData(userWalletId = userWalletId, cardId = CARD_ID)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GetProvisioningDataModel(
                response = processorDetails(cardSecret = CARD_SECRET),
                expected = TangemPayCardProvisioningData(
                    processorCardId = PROCESSOR_CARD_ID,
                    cardSecret = CARD_SECRET,
                ).right(),
            ),
            GetProvisioningDataModel(
                response = processorDetails(cardSecret = null),
                expected = VisaApiError.Unspecified.left(),
            ),
            GetProvisioningDataModel(
                response = processorDetails(cardSecret = " "),
                expected = VisaApiError.Unspecified.left(),
            ),
            GetProvisioningDataModel(
                response = ApiResponse.Success(CardProcessorDetailsResponse(result = null)),
                expected = VisaApiError.Unspecified.left(),
            ),
            GetProvisioningDataModel(
                response = ApiResponse.Error(ApiResponseError.NetworkException())
                    as ApiResponse<CardProcessorDetailsResponse>,
                expected = VisaApiError.ServerUnavailable.left(),
            ),
        )

        private fun processorDetails(cardSecret: String?): ApiResponse<CardProcessorDetailsResponse> {
            return ApiResponse.Success(
                CardProcessorDetailsResponse(
                    result = CardProcessorDetailsResponse.Result(cardId = PROCESSOR_CARD_ID, cardSecret = cardSecret),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsProductionEnvironment {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Pay environment WHEN isProductionEnvironment THEN only PROD is production`(
            model: EnvironmentModel,
        ) {
            // Arrange
            every { apiConfigsManager.getEnvironmentConfig(TangemPay.Bff.ID) } returns ApiEnvironmentConfig(
                environment = model.environment,
                baseUrl = BASE_URL,
            )

            // Act
            val actual = repository.isProductionEnvironment()

            // Assert
            assertThat(actual).isEqualTo(model.isProduction)
        }

        private fun provideTestModels() = listOf(
            EnvironmentModel(environment = ApiEnvironment.PROD, isProduction = true),
            EnvironmentModel(environment = ApiEnvironment.DEV, isProduction = false),
            EnvironmentModel(environment = ApiEnvironment.STAGE, isProduction = false),
            EnvironmentModel(environment = ApiEnvironment.MOCK, isProduction = false),
        )
    }

    internal data class GetProvisioningDataModel(
        val response: ApiResponse<CardProcessorDetailsResponse>,
        val expected: Either<UniversalError, TangemPayCardProvisioningData>,
    )

    internal data class EnvironmentModel(val environment: ApiEnvironment, val isProduction: Boolean)

    private companion object {
        const val AUTH_HEADER = "Bearer token"
        const val CARD_ID = "card-id"
        const val PROCESSOR_CARD_ID = "processor-card-id"
        const val CARD_SECRET = "time-based-secret"
        const val BASE_URL = "https://pay.test/"
    }
}