package com.tangem.data.visa

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.spend.datasource.visa.VisaApi
import com.tangem.spend.datasource.visa.models.response.CardActivationRemoteStateResponse
import com.tangem.spend.datasource.visa.storage.VisaAuthTokenStorage
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultVisaActivationRepositoryTest {

    private val visaApi: VisaApi = mockk()
    private val visaAuthTokenStorage: VisaAuthTokenStorage = mockk()

    private val repository = DefaultVisaActivationRepository(
        visaCardId = VisaCardId(cardId = CARD_ID, cardPublicKey = CARD_PUBLIC_KEY),
        json = NetworkJsonFactory.create(),
        visaApi = visaApi,
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
        visaAuthTokenStorage = visaAuthTokenStorage,
        visaAuthRemoteDataSource = mockk(),
        visaLibLoader = mockk(),
        apiConfigsManager = mockk(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(visaApi, visaAuthTokenStorage)
        coEvery { visaAuthTokenStorage.get(CARD_ID) } returns AUTH_TOKENS
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN remote activation state WHEN getActivationRemoteState THEN converted or an error returned`(
        model: StateModel,
    ) = runTest {
        // Arrange
        coEvery { visaApi.getRemoteActivationStatus(authHeader = any(), request = any()) } returns
            ApiResponse.Success(CardActivationRemoteStateResponse(result = model.result))

        // Act
        val actual = repository.getActivationRemoteState()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        StateModel(
            name = "activated",
            result = result(status = "activated"),
            expected = VisaActivationRemoteState.Activated.right(),
        ),
        StateModel(
            name = "signature required with an order",
            result = result(status = "card_wallet_signature_required", order = ORDER),
            expected = VisaActivationRemoteState.CardWalletSignatureRequired(activationOrderInfo = ORDER_INFO).right(),
        ),
        StateModel(
            name = "signature required without an order",
            result = result(status = "card_wallet_signature_required"),
            expected = VisaApiError.UnknownWithoutCode.left(),
        ),
        StateModel(
            name = "unknown status",
            result = result(status = "awaiting_something_new"),
            expected = VisaApiError.UnknownWithoutCode.left(),
        ),
    )

    private fun result(status: String, order: CardActivationRemoteStateResponse.ActivationOrder? = null) =
        CardActivationRemoteStateResponse.Result(status = status, activationOrder = order, stepError = null)

    internal data class StateModel(
        val name: String,
        val result: CardActivationRemoteStateResponse.Result,
        val expected: Either<VisaApiError, VisaActivationRemoteState>,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val CARD_ID = "card-1"
        const val CARD_PUBLIC_KEY = "public-key"
        val AUTH_TOKENS = VisaAuthTokens(
            accessToken = "access",
            refreshToken = VisaAuthTokens.RefreshToken(
                value = "refresh",
                authType = VisaAuthTokens.RefreshToken.Type.CardWallet,
            ),
        )
        val ORDER = CardActivationRemoteStateResponse.ActivationOrder(
            id = "order-1",
            customerId = "customer-1",
            customerWalletAddress = "0xcustomer",
            cardWalletAddress = "0xcard",
        )
        val ORDER_INFO = VisaActivationOrderInfo(
            orderId = "order-1",
            customerId = "customer-1",
            customerWalletAddress = "0xcustomer",
            cardWalletAddress = "0xcard",
        )
    }
}