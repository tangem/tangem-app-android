package com.tangem.data.common.currency

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTokensSaverTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userTokensResponseStore: UserTokensResponseStore = mockk(relaxed = true)
    private val enricher: UserTokensResponseAddressesEnricher = mockk()

    private val userTokensSaver: UserTokensSaver = UserTokensSaver(
        tangemTechApi = tangemTechApi,
        userTokensResponseStore = userTokensResponseStore,
        userTokensResponseAddressesEnricher = enricher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemTechApi, userTokensResponseStore, enricher)
    }

    @Test
    fun `GIVEN user wallet id and response WHEN store THEN should store enriched response`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("1234567890abcdef")
        val response = UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )

        val enrichedResponse = UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = emptyList(),
        )

        coEvery { enricher(userWalletId, response) } returns enrichedResponse

        // WHEN
        userTokensSaver.store(userWalletId, response)

        // THEN
        coVerifyOrder {
            enricher(userWalletId, response)
            userTokensResponseStore.store(userWalletId, enrichedResponse)
        }

        coVerify(inverse = true) {
            tangemTechApi.saveUserTokens(any(), any())
        }
    }

    @Test
    fun `GIVEN user wallet id and response WHEN push AND api call fails THEN should log error and call onFailSend`() =
        runTest {
            // GIVEN
            val userWalletId = UserWalletId("1234567890abcdef")
            val response = UserTokensResponse(
                version = 0,
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.BALANCE,
                tokens = emptyList(),
            )
            val enrichedResponse = UserTokensResponse(
                version = 0,
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.MANUAL,
                tokens = emptyList(),
            )
            val error = ApiResponseError.UnknownException(Exception("API Error"))
            var onFailSendCalled = false
            coEvery { enricher(userWalletId, response) } returns enrichedResponse
            coEvery { tangemTechApi.saveUserTokens(any(), any()) } returns ApiResponse.Error(error) as ApiResponse<Unit>

            // WHEN
            userTokensSaver.push(
                userWalletId = userWalletId,
                response = response,
                onFailSend = { onFailSendCalled = true },
            )

            // THEN
            coVerifyOrder {
                enricher(userWalletId, response)
                tangemTechApi.saveUserTokens(userWalletId.stringValue, enrichedResponse)
            }

            assert(onFailSendCalled) { "onFailSend callback should be called when API call fails" }
        }

    @Test
    fun `GIVEN user wallet id and response WHEN storeAndPush THEN should store and push enriched response`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("1234567890abcdef")
        val response = UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )
        val enrichedResponse = UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )
        coEvery { enricher(userWalletId, response) } returns enrichedResponse
        coEvery {
            tangemTechApi.saveUserTokens(userWalletId.stringValue, enrichedResponse)
        } returns ApiResponse.Success(Unit)

        // WHEN
        userTokensSaver.storeAndPush(userWalletId, response)

        // THEN
        coVerifyOrder {
            enricher(userWalletId, response)
            tangemTechApi.saveUserTokens(userWalletId.stringValue, enrichedResponse)
        }
    }
}