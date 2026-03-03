package com.tangem.data.common.currency

import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTokensSaverTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxUnitFun = true)
    private val userTokensResponseStore: UserTokensResponseStore = mockk(relaxed = true)
    private val enricher: UserTokensResponseAddressesEnricher = mockk()
    private val walletServerBinder: WalletServerBinder = mockk()

    private val userTokensSaver: UserTokensSaver = UserTokensSaver(
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = userWalletsListRepository,
        userTokensResponseStore = userTokensResponseStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
        addressesEnricher = enricher,
        walletServerBinder = walletServerBinder,
        pushTokensRetryerPool = mockk(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            tangemTechApi,
            userWalletsListRepository,
            userTokensResponseStore,
            enricher,
            walletServerBinder,
        )
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
            tangemTechApi.saveTokens(any(), any())
        }
    }

    @Test
    fun `GIVEN user wallet id and response WHEN push AND api call fails THEN should log error and call onFailSend`() =
        runTest {
            // GIVEN
            val userWalletId = UserWalletId("1234567890abcdef")
            val userWallet = mockk<UserWallet.Cold> {
                every { this@mockk.walletId } returns userWalletId
                every { this@mockk.name } returns ""
            }

            val response = UserTokensResponse(
                version = 0,
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.BALANCE,
                tokens = emptyList(),
                walletName = null,
                walletType = WalletType.COLD,
            )
            val enrichedResponse = UserTokensResponse(
                version = 0,
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.MANUAL,
                tokens = emptyList(),
                walletName = null,
                walletType = WalletType.COLD,
            )
            val error = ApiResponseError.UnknownException(Exception("API Error"))
            var onFailSendCalled = false

            val userWalletsFlow = MutableStateFlow(listOf(userWallet))

            every { userWalletsListRepository.userWallets } returns userWalletsFlow
            coEvery { enricher(userWalletId, response) } returns enrichedResponse
            coEvery { tangemTechApi.saveTokens(any(), any()) } returns ApiResponse.Error(error) as ApiResponse<Unit>

            // WHEN
            userTokensSaver.push(
                userWalletId = userWalletId,
                response = response,
                onFailSend = { onFailSendCalled = true },
            )

            // THEN
            coVerifyOrder {
                userWalletsListRepository.userWallets
                enricher(userWalletId, response)
                tangemTechApi.saveTokens(userWalletId.stringValue, enrichedResponse)
            }

            assert(onFailSendCalled) { "onFailSend callback should be called when API call fails" }
        }

    @Test
    fun `GIVEN user wallet id and response WHEN storeAndPush THEN should store and push enriched response`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("1234567890abcdef")
        val userWallet = mockk<UserWallet.Cold> {
            every { this@mockk.walletId } returns userWalletId
            every { this@mockk.name } returns "Wallet"
        }

        val response = UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
            walletName = userWallet.name,
            walletType = WalletType.COLD,
        )
        val enrichedResponse = UserTokensResponse(
            version = 0,
            group = UserTokensResponse.GroupType.NETWORK,
            sort = UserTokensResponse.SortType.BALANCE,
            tokens = emptyList(),
            walletName = userWallet.name,
            walletType = WalletType.COLD,
        )

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { enricher(userWalletId, response) } returns enrichedResponse
        coEvery {
            tangemTechApi.saveTokens(userWalletId.stringValue, enrichedResponse)
        } returns ApiResponse.Success(Unit)

        // WHEN
        userTokensSaver.storeAndPush(userWalletId, response)

        // THEN
        coVerifyOrder {
            enricher(userWalletId, response)
            userWalletsListRepository.userWallets
            tangemTechApi.saveTokens(userWalletId.stringValue, enrichedResponse)
        }
    }
}