package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.FeeResponse
import com.tangem.spend.datasource.pay.store.TangemPayReissueCardStore
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultReissueCardRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()
    private val reissueCardStore: TangemPayReissueCardStore = mockk(relaxed = true)

    private val repository = DefaultReissueCardRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
        tangemPayReissueCardStore = reissueCardStore,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper, reissueCardStore)
        coEvery { reissueCardStore.getReissueFee(any()) } returns null
        coEvery {
            requestHelper.performRequest<Any>(userWalletId = any(), requestBlock = any())
        } coAnswers {
            val block = secondArg<suspend (String) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee amount WHEN getReissueCardFee THEN it is parsed or an error is returned`(model: FeeModel) = runTest {
        // Arrange
        coEvery { tangemPayApi.getFee(authHeader = AUTH_HEADER, type = any()) } returns
            ApiResponse.Success(feeResponse(amount = model.amount))

        // Act
        val actual = repository.getReissueCardFee(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FeeModel(
            amount = "5.00",
            expected = TangemPayReissueCardFee(amount = BigDecimal("5.00"), currencyCode = CURRENCY).right(),
        ),
        FeeModel(amount = "five", expected = VisaApiError.UnknownWithoutCode.left()),
        FeeModel(amount = "", expected = VisaApiError.UnknownWithoutCode.left()),
    )

    private fun feeResponse(amount: String) = FeeResponse(
        result = FeeResponse.Result(
            type = "CARD_REPLACEMENT",
            amount = amount,
            currency = CURRENCY,
            description = "Card replacement",
        ),
    )

    internal data class FeeModel(val amount: String, val expected: Either<VisaApiError, TangemPayReissueCardFee>) {
        override fun toString(): String = "amount '$amount'"
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val AUTH_HEADER = "auth-header"
        const val CURRENCY = "USD"
    }
}