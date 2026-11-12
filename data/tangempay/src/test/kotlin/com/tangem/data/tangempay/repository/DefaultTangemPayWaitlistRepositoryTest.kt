package com.tangem.data.tangempay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayWaitlistEntry
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.models.request.WaitlistRequest
import com.tangem.spend.datasource.tangempay.models.response.WaitlistResponse
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayWaitlistRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()

    private val repository = DefaultTangemPayWaitlistRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper)

        coEvery { requestHelper.performWithOptionalToken<Any>(userWalletId = any(), requestBlock = any()) } coAnswers {
            val block = secondArg<suspend (String?) -> ApiResponse<Any>>()
            when (val response = block(AUTH_HEADER)) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> TangemPayApiError.Unspecified.left()
            }
        }
        coEvery { tangemPayApi.joinWaitlist(any(), any(), any()) } returns ApiResponse.Success(WAITLIST_RESPONSE)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN api succeeds WHEN join THEN maps product and entry to request and returns Unit`(model: JoinModel) =
        runTest {
            // Act
            val actual = repository.join(
                userWalletId = USER_WALLET_ID,
                product = model.product,
                entry = model.entry,
                idempotencyKey = IDEMPOTENCY_KEY,
            )

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                tangemPayApi.joinWaitlist(
                    authHeader = AUTH_HEADER,
                    idempotencyKey = IDEMPOTENCY_KEY,
                    body = model.expectedBody,
                )
            }
        }

    @Test
    fun `GIVEN no user wallet WHEN join THEN request is performed with null userWalletId`() = runTest {
        // Act
        val actual = repository.join(
            userWalletId = null,
            product = TangemPayProduct.BANKING_ACCOUNT,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            requestHelper.performWithOptionalToken<WaitlistResponse>(userWalletId = null, requestBlock = any())
        }
    }

    @Test
    fun `GIVEN api fails WHEN join THEN returns error`() = runTest {
        // Arrange
        coEvery { tangemPayApi.joinWaitlist(any(), any(), any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<WaitlistResponse>

        // Act
        val actual = repository.join(
            userWalletId = USER_WALLET_ID,
            product = TangemPayProduct.BANKING_ACCOUNT,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.Unspecified.left())
    }

    private fun provideTestModels() = listOf(
        JoinModel(
            product = TangemPayProduct.BANKING_ACCOUNT,
            entry = ENTRY,
            expectedBody = WaitlistRequest(
                email = "user@tangem.com",
                product = "BANKING_ACCOUNT",
                country = "US",
                region = "CA",
                hasMarketingConsent = true,
            ),
        ),
        JoinModel(
            product = TangemPayProduct.PAYMENT_ACCOUNT,
            entry = ENTRY.copy(
                regionCode = null,
                isMarketingConsentGiven = false,
            ),
            expectedBody = WaitlistRequest(
                email = "user@tangem.com",
                product = "PAYMENT_ACCOUNT",
                country = "US",
                region = null,
                hasMarketingConsent = false,
            ),
        ),
    )

    internal data class JoinModel(
        val product: TangemPayProduct,
        val entry: TangemPayWaitlistEntry,
        val expectedBody: WaitlistRequest,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val IDEMPOTENCY_KEY = "idempotency-key-1"
        const val AUTH_HEADER = "Bearer token"

        val ENTRY = TangemPayWaitlistEntry(
            email = "user@tangem.com",
            countryCode = "US",
            regionCode = "CA",
            isMarketingConsentGiven = true,
        )

        val WAITLIST_RESPONSE = WaitlistResponse(
            result = WaitlistResponse.Result(status = "JOINED", joinedAt = "2026-09-14T00:00:00Z"),
        )
    }
}