package com.tangem.data.onramp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
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
}