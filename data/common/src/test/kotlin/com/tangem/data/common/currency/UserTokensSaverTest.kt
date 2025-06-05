package com.tangem.data.common.currency

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserTokensSaverTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)
    private val appPreferenceStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = dataStore,
    )
    private val dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider()
    private val userTokensResponseAddressesEnricher: UserTokensResponseAddressesEnricher = mockk()

    private lateinit var userTokensSaver: UserTokensSaver

    @Before
    fun setup() {
        coEvery { dataStore.data } returns flowOf(mockk(relaxed = true))
        userTokensSaver = UserTokensSaver(
            tangemTechApi = tangemTechApi,
            appPreferencesStore = appPreferenceStore,
            dispatchers = dispatchers,
            userTokensResponseAddressesEnricher = userTokensResponseAddressesEnricher,
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
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
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
        )
        coEvery { userTokensResponseAddressesEnricher(userWalletId, response) } returns enrichedResponse
        coEvery { dataStore.updateData(any()) } returns mockk(relaxed = true)

        // WHEN
        userTokensSaver.store(userWalletId, response)

        // THEN
        coVerify {
            dataStore.updateData(any())
        }
        coVerify(exactly = 0) {
            tangemTechApi.saveUserTokens(any(), any())
        }
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
        coEvery { userTokensResponseAddressesEnricher(userWalletId, response) } returns enrichedResponse
        coEvery { dataStore.updateData(any()) } returns mockk(relaxed = true)
        coEvery { tangemTechApi.saveUserTokens(any(), any()) } returns ApiResponse.Success(Unit)

        // WHEN
        userTokensSaver.storeAndPush(userWalletId, response)

        // THEN
        coVerify {
            dataStore.updateData(any())
            tangemTechApi.saveUserTokens(userWalletId.stringValue, enrichedResponse)
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
                sort = UserTokensResponse.SortType.BALANCE,
                tokens = emptyList(),
            )
            val error = ApiResponseError.UnknownException(Exception("API Error"))
            var onFailSendCalled = false
            coEvery { userTokensResponseAddressesEnricher(userWalletId, response) } returns enrichedResponse
            coEvery { tangemTechApi.saveUserTokens(any(), any()) } returns
                ApiResponse.Error(error) as ApiResponse<Unit>

            // WHEN
            userTokensSaver.push(
                userWalletId = userWalletId,
                response = response,
                onFailSend = { onFailSendCalled = true },
            )

            // THEN
            coVerify {
                tangemTechApi.saveUserTokens(userWalletId.stringValue, enrichedResponse)
            }
            coVerify(exactly = 0) {
                dataStore.updateData(any())
            }
            assert(onFailSendCalled) { "onFailSend callback should be called when API call fails" }
        }
}