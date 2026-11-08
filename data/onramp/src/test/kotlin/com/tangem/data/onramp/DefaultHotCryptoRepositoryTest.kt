package com.tangem.data.onramp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.exchangeservice.hotcrypto.HotCryptoResponseStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class DefaultHotCryptoRepositoryTest {

    private val store = HotCryptoResponseStore()
    private val wallets = mockk<UserWalletsListRepository>()
    private val walletId = UserWalletId("0011")
    private val wallet = mockk<UserWallet.Hot> { every { walletId } returns this@DefaultHotCryptoRepositoryTest.walletId }

    @Test
    fun `GIVEN failed hot crypto request WHEN fetch starts THEN USD fallback requested and error reported`() = runTest {
        // Arrange
        val api = mockk<com.tangem.datasource.api.tangemTech.TangemTechApi>()
        val appCurrency = mockk<com.tangem.datasource.appcurrency.AppCurrencyResponseStore>()
        val analytics = mockk<com.tangem.core.analytics.api.AnalyticsEventHandler>(relaxed = true)
        every { wallets.userWallets } returns MutableStateFlow(emptyList())
        coEvery { wallets.load() } returns Unit
        every { appCurrency.get() } returns flowOf(null)
        coEvery { api.getHotCrypto("usd") } throws IllegalStateException("offline")
        val repository = DefaultHotCryptoRepository(
            excludedBlockchains = ExcludedBlockchains(), hotCryptoResponseStore = store,
            userWalletsListRepository = wallets, tangemTechApi = api, appCurrencyResponseStore = appCurrency,
            walletAccountsFetcher = mockk(), dispatchers = createTestingCoroutineDispatcherProvider(),
            analyticsEventHandler = analytics, appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
        )
        // Act
        repository.fetchHotCrypto()
        runCurrent()
        // Assert
        coVerify(exactly = 1) { api.getHotCrypto("usd") }
        verify(exactly = 1) {
            analytics.send(match { it is com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent.HotTokenError })
        }
    }

    @Test
    fun `GIVEN wallet scoped responses WHEN currencies observed THEN missing wallet skipped and invalid tokens discarded`() = runTest {
        // Arrange
        every { wallets.userWallets } returns MutableStateFlow(listOf(wallet))
        val repository = DefaultHotCryptoRepository(
            excludedBlockchains = ExcludedBlockchains(), hotCryptoResponseStore = store,
            userWalletsListRepository = wallets, tangemTechApi = mockk(), appCurrencyResponseStore = mockk(),
            walletAccountsFetcher = mockk(), dispatchers = createTestingCoroutineDispatcherProvider(),
            analyticsEventHandler = mockk(), appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
        )
        // Act
        repository.getCurrencies(walletId).test {
            expectNoEvents()
            store.store(mapOf(UserWalletId("0022") to HotCryptoResponse(tokens = emptyList())))
            expectNoEvents()
            store.store(mapOf(walletId to HotCryptoResponse(tokens = listOf(HotCryptoResponse.Token()))))
            // Assert
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN supported and owned hot tokens WHEN fetched twice THEN wallet scoped filtering and one subscription remain`() =
        runTest {
            // Arrange
            val otherId = UserWalletId("0022")
            val otherWallet = mockk<UserWallet.Hot> { every { walletId } returns otherId }
            every { wallets.userWallets } returns MutableStateFlow(listOf(wallet, otherWallet))
            coEvery { wallets.load() } returns Unit
            val owned = createHotToken(id = "tether", contract = "0x1")
            val otherContract = owned.copy(contractAddress = "0x2")
            val otherNetwork = owned.copy(networkId = "polygon-pos")
            val polygon = createHotToken(id = "matic-network", network = "polygon-pos")
            val response = HotCryptoResponse(
                imageHost = "https://images/",
                tokens = listOf(
                    owned, otherContract, otherNetwork, polygon,
                    createHotToken(network = "unknown"), createHotToken(network = "ethereum/test"),
                ),
            )
            val api = mockk<com.tangem.datasource.api.tangemTech.TangemTechApi>()
            coEvery { api.getHotCrypto("usd") } returns ApiResponse.Success(response)
            val appCurrency = mockk<com.tangem.datasource.appcurrency.AppCurrencyResponseStore>()
            every { appCurrency.get() } returns flowOf(null)
            val fetcher = mockk<WalletAccountsFetcher>()
            val accountSource = MutableStateFlow(createAccounts(listOf(owned)))
            val otherSource = MutableStateFlow(createAccounts(emptyList()))
            every { fetcher.get(walletId) } returns accountSource
            every { fetcher.get(otherId) } returns otherSource
            val analytics = mockk<com.tangem.core.analytics.api.AnalyticsEventHandler>(relaxUnitFun = true)
            val repository = DefaultHotCryptoRepository(
                excludedBlockchains = ExcludedBlockchains(), hotCryptoResponseStore = store,
                userWalletsListRepository = wallets, tangemTechApi = api, appCurrencyResponseStore = appCurrency,
                walletAccountsFetcher = fetcher, dispatchers = createTestingCoroutineDispatcherProvider(),
                analyticsEventHandler = analytics, appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
            )
            val migrated = polygon.copy(id = "polygon-ecosystem-token")

            // Act
            repository.fetchHotCrypto()
            runCurrent()
            repository.fetchHotCrypto()
            runCurrent()

            // Assert
            assertThat(store.get().first()).isEqualTo(
                mapOf(
                    walletId to response.copy(tokens = listOf(otherContract, otherNetwork, migrated)),
                    otherId to response.copy(tokens = listOf(owned, otherContract, otherNetwork, migrated)),
                ),
            )
            assertThat(accountSource.subscriptionCount.value).isEqualTo(1)
            assertThat(otherSource.subscriptionCount.value).isEqualTo(1)
            coVerify(exactly = 2) { api.getHotCrypto("usd") }
            verify(exactly = 0) { analytics.send(any()) }

            // Act
            accountSource.value = createAccounts(listOf(owned, otherContract))
            runCurrent()

            // Assert
            assertThat(store.get().first()[walletId]).isEqualTo(response.copy(tokens = listOf(otherNetwork, migrated)))
            coVerify(exactly = 2) { api.getHotCrypto("usd") }
        }

    private fun createHotToken(
        id: String = "ethereum",
        network: String = "ethereum",
        contract: String? = null,
    ) = HotCryptoResponse.Token(
        id = id, name = id, symbol = "ETH", networkId = network,
        contractAddress = contract, decimalCount = if (contract == null) null else 18,
    )

    private fun createAccounts(tokens: List<HotCryptoResponse.Token> = emptyList()): GetWalletAccountsResponse {
        val account = mockk<WalletAccountDTO> {
            every { this@mockk.tokens } returns tokens.map {
                UserTokensResponse.Token(
                    id = it.id, networkId = it.networkId.orEmpty(), name = it.name.orEmpty(),
                    symbol = it.symbol.orEmpty(), decimals = 18, contractAddress = it.contractAddress,
                )
            }
        }
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(group = null, sort = null, totalAccounts = 1, totalArchivedAccounts = 0),
            accounts = listOf(account), unassignedTokens = emptyList(),
        )
    }
}