package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.datasource.api.pay.models.response.TariffPlanTransitionResponse
import com.tangem.datasource.api.pay.models.response.TariffPlanTransitionsResponse
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultTariffPlanTransitionsRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()

    private val repository = DefaultTariffPlanTransitionsRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
    )

    @BeforeEach
    fun setUp() {
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

    @Test
    fun `GIVEN backend error WHEN getTransitions THEN returns error`() = runTest {
        // GIVEN
        coEvery { tangemPayApi.getTariffPlanTransitions(any()) } returns
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<TariffPlanTransitionsResponse>

        // WHEN
        val result = repository.getTransitions(USER_WALLET_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    @Test
    fun `GIVEN null result WHEN getTransitions THEN returns empty list`() = runTest {
        // GIVEN
        coEvery { tangemPayApi.getTariffPlanTransitions(any()) } returns
            ApiResponse.Success(TariffPlanTransitionsResponse(result = null))

        // WHEN
        val result = repository.getTransitions(USER_WALLET_ID)

        // THEN
        assertThat(result.getOrNull()).isEqualTo(emptyList<TangemPayTariffPlanTransition>())
    }

    @Test
    fun `GIVEN valid transitions WHEN getTransitions THEN maps them to domain`() = runTest {
        // GIVEN
        coEvery { tangemPayApi.getTariffPlanTransitions(any()) } returns ApiResponse.Success(
            TariffPlanTransitionsResponse(
                result = listOf(
                    TariffPlanTransitionResponse(type = "UPGRADE", tariffPlan = tariffPlan()),
                ),
            ),
        )

        // WHEN
        val result = repository.getTransitions(USER_WALLET_ID)

        // THEN
        val expected = listOf(
            TangemPayTariffPlanTransition(
                type = TangemPayTariffPlanTransition.Type.UPGRADE,
                plan = TangemPayTariffPlan(
                    id = PLAN_ID,
                    type = TangemPayTariffPlan.Type.PLUS,
                    name = PLAN_NAME,
                    descriptionItems = emptyList(),
                    images = emptyList(),
                    fees = emptyList(),
                ),
            ),
        )
        assertThat(result.getOrNull()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a transition with unconvertible plan WHEN getTransitions THEN it is filtered out`() = runTest {
        // GIVEN
        coEvery { tangemPayApi.getTariffPlanTransitions(any()) } returns ApiResponse.Success(
            TariffPlanTransitionsResponse(
                result = listOf(
                    TariffPlanTransitionResponse(type = "UPGRADE", tariffPlan = null),
                    TariffPlanTransitionResponse(type = "DOWNGRADE", tariffPlan = tariffPlan(id = null)),
                    TariffPlanTransitionResponse(type = "ACTIVATION", tariffPlan = tariffPlan()),
                ),
            ),
        )

        // WHEN
        val result = repository.getTransitions(USER_WALLET_ID)

        // THEN
        val transitions = result.getOrNull().orEmpty()
        assertThat(transitions.map { it.type })
            .containsExactly(TangemPayTariffPlanTransition.Type.ACTIVATION)
    }

    private fun tariffPlan(id: String? = PLAN_ID) = CustomerMeResponse.TariffPlan(
        id = id,
        type = "PLUS",
        name = PLAN_NAME,
        descriptionItems = null,
        images = null,
        fees = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val AUTH_HEADER = "auth-header"
        const val PLAN_ID = "plan-plus"
        const val PLAN_NAME = "Plus"
    }
}