package com.tangem.features.feed.deeplink

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetAvailabilityUseCase
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultYieldDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier = mockk()
    private val yieldSupplyGetAvailabilityUseCase: YieldSupplyGetAvailabilityUseCase = mockk()

    private val tokenId = "usd-coin"
    private val networkId = "base"

    @BeforeEach
    fun setUp() {
        mockkObject(TangemLogger)
        every { appRouter.push(any(), any()) } just Runs
    }

    @Test
    fun `missing token_id falls back to earn yield`() = runTest {
        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(NETWORK_ID_KEY to networkId),
        )

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = networkId,
                ),
                any(),
            )
        }
    }

    @Test
    fun `missing network_id falls back to earn yield`() = runTest {
        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(TOKEN_ID_KEY to tokenId),
        )

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = null,
                ),
                any(),
            )
        }
    }

    @Test
    fun `no selected wallet falls back to earn yield`() = runTest {
        every { getSelectedWalletSyncUseCase() } returns Either.Left(mockk())

        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(TOKEN_ID_KEY to tokenId, NETWORK_ID_KEY to networkId),
        )
        advanceUntilIdle()

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = networkId,
                ),
                any(),
            )
        }
    }

    @Test
    fun `token not in wallet falls back to earn yield`() = runTest {
        val walletId = UserWalletId("011")
        val wallet = mockk<UserWallet> { every { this@mockk.walletId } returns walletId }
        every { getSelectedWalletSyncUseCase() } returns Either.Right(wallet)
        coEvery {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                MultiWalletCryptoCurrenciesProducer.Params(walletId),
            )
        } returns emptySet()

        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(TOKEN_ID_KEY to tokenId, NETWORK_ID_KEY to networkId),
        )
        advanceUntilIdle()

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = networkId,
                ),
                any(),
            )
        }
    }

    @Test
    fun `eligible token with yield available pushes YieldSupplyEntry`() = runTest {
        val walletId = UserWalletId("011")
        val wallet = mockk<UserWallet> { every { this@mockk.walletId } returns walletId }
        val currency = mockEligibleCurrency()
        every { getSelectedWalletSyncUseCase() } returns Either.Right(wallet)
        coEvery {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                MultiWalletCryptoCurrenciesProducer.Params(walletId),
            )
        } returns setOf(currency)
        coEvery { yieldSupplyGetAvailabilityUseCase(currency) } returns
            Either.Right(YieldSupplyAvailability.Available(apy = "2.66"))

        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(TOKEN_ID_KEY to tokenId, NETWORK_ID_KEY to networkId),
        )
        advanceUntilIdle()

        verify {
            appRouter.push(
                match<AppRoute.YieldSupplyEntry> {
                    it.userWalletId == walletId && it.cryptoCurrency === currency && it.apy == "2.66"
                },
                any(),
            )
        }
    }

    @Test
    fun `eligible token with yield unavailable falls back to earn yield`() = runTest {
        val walletId = UserWalletId("011")
        val wallet = mockk<UserWallet> { every { this@mockk.walletId } returns walletId }
        val currency = mockEligibleCurrency()
        every { getSelectedWalletSyncUseCase() } returns Either.Right(wallet)
        coEvery {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                MultiWalletCryptoCurrenciesProducer.Params(walletId),
            )
        } returns setOf(currency)
        coEvery { yieldSupplyGetAvailabilityUseCase(currency) } returns
            Either.Right(YieldSupplyAvailability.Unavailable)

        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(TOKEN_ID_KEY to tokenId, NETWORK_ID_KEY to networkId),
        )
        advanceUntilIdle()

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = networkId,
                ),
                any(),
            )
        }
    }

    @Test
    fun `eligible token with availability lookup error falls back to earn yield`() = runTest {
        val walletId = UserWalletId("011")
        val wallet = mockk<UserWallet> { every { this@mockk.walletId } returns walletId }
        val currency = mockEligibleCurrency()
        every { getSelectedWalletSyncUseCase() } returns Either.Right(wallet)
        coEvery {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                MultiWalletCryptoCurrenciesProducer.Params(walletId),
            )
        } returns setOf(currency)
        coEvery { yieldSupplyGetAvailabilityUseCase(currency) } returns
            Either.Left(IllegalStateException("api 500"))

        handle(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryParams = mapOf(TOKEN_ID_KEY to tokenId, NETWORK_ID_KEY to networkId),
        )
        advanceUntilIdle()

        verify {
            appRouter.push(
                AppRoute.Earn(
                    preselectedEarnType = PreselectedEarnType.Yield,
                    preselectedNetworkId = networkId,
                ),
                any(),
            )
        }
    }

    /** Builds a [CryptoCurrency] mock whose `network.rawId` and `id.rawCurrencyId` match the test's
     *  [tokenId] / [networkId] — i.e. the supplier-lookup predicate inside the handler returns it. */
    private fun mockEligibleCurrency(): CryptoCurrency = mockk(relaxed = true) {
        every { network } returns mockk<Network>(relaxed = true) { every { rawId } returns networkId }
        every { id } returns mockk(relaxed = true) {
            every { rawCurrencyId } returns CryptoCurrency.RawID(tokenId)
        }
    }

    private fun handle(scope: CoroutineScope, queryParams: Map<String, String>) {
        DefaultYieldDeepLinkHandler(
            scope = scope,
            queryParams = queryParams,
            appRouter = appRouter,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            yieldSupplyGetAvailabilityUseCase = yieldSupplyGetAvailabilityUseCase,
        )
    }
}