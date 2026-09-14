package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.WaitlistRequest
import com.tangem.spend.datasource.pay.models.response.WaitlistResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery { tangemPayApi.joinWaitlist(any(), any(), any()) } returns ApiResponse.Success(WAITLIST_RESPONSE)
    }

    @Test
    fun `GIVEN api succeeds WHEN join THEN maps entry to request and returns Unit`() = runTest {
        // WHEN
        val actual = repository.join(
            userWalletId = USER_WALLET_ID,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // THEN
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            tangemPayApi.joinWaitlist(
                authHeader = AUTH_HEADER,
                idempotencyKey = IDEMPOTENCY_KEY,
                body = WaitlistRequest(
                    email = "user@tangem.com",
                    product = "BANKING_ACCOUNT",
                    country = "US",
                    region = "CA",
                    hasMarketingConsent = true,
                ),
            )
        }
    }

    @Test
    fun `GIVEN payment account entry without region WHEN join THEN request carries null region`() = runTest {
        // GIVEN
        val entry = ENTRY.copy(
            product = TangemPayProduct.PAYMENT_ACCOUNT,
            regionCode = null,
            isMarketingConsentGiven = false,
        )

        // WHEN
        val actual = repository.join(
            userWalletId = USER_WALLET_ID,
            entry = entry,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // THEN
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            tangemPayApi.joinWaitlist(
                authHeader = AUTH_HEADER,
                idempotencyKey = IDEMPOTENCY_KEY,
                body = WaitlistRequest(
                    email = "user@tangem.com",
                    product = "PAYMENT_ACCOUNT",
                    country = "US",
                    region = null,
                    hasMarketingConsent = false,
                ),
            )
        }
    }

    @Test
    fun `GIVEN no user wallet WHEN join THEN request is performed with null userWalletId`() = runTest {
        // WHEN
        val actual = repository.join(userWalletId = null, entry = ENTRY, idempotencyKey = IDEMPOTENCY_KEY)

        // THEN
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            requestHelper.performWithOptionalToken<WaitlistResponse>(userWalletId = null, requestBlock = any())
        }
    }

    @Test
    fun `GIVEN api fails WHEN join THEN returns error`() = runTest {
        // GIVEN
        coEvery { tangemPayApi.joinWaitlist(any(), any(), any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<WaitlistResponse>

        // WHEN
        val actual = repository.join(
            userWalletId = USER_WALLET_ID,
            entry = ENTRY,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

        // THEN
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val IDEMPOTENCY_KEY = "idempotency-key-1"
        const val AUTH_HEADER = "Bearer token"

        val ENTRY = TangemPayWaitlistEntry(
            email = "user@tangem.com",
            product = TangemPayProduct.BANKING_ACCOUNT,
            countryCode = "US",
            regionCode = "CA",
            isMarketingConsentGiven = true,
        )

        val WAITLIST_RESPONSE = WaitlistResponse(
            result = WaitlistResponse.Result(status = "JOINED", joinedAt = "2026-09-14T00:00:00Z"),
        )
    }
}