package com.tangem.data.networks.single

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStoreV2
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusFetcherTest {

    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    private val networksStatusesStore: NetworksStatusesStoreV2 = mockk(relaxed = true)
    private val userWalletsStore: UserWalletsStore = mockk(relaxed = true)

    private val fetcher = DefaultSingleNetworkStatusFetcher(
        excludedBlockchains = mockk(relaxed = true),
        walletManagersFacade = walletManagersFacade,
        networksStatusesStore = networksStatusesStore,
        userWalletsStore = userWalletsStore,
        appPreferencesStore = mockk(relaxed = true),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `fetch network status successfully`() = runTest {
        val params = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = network,
            applyRefresh = true,
        )

        val result = UpdateWalletManagerResult.MissedDerivation
        coEvery { walletManagersFacade.update(userWalletId, network, emptySet()) } returns result

        val actual = fetcher(params)

        coVerifyOrder {
            networksStatusesStore.refresh(userWalletId = userWalletId, network = network)
            userWalletsStore.getSyncStrict(key = userWalletId)
            walletManagersFacade.update(userWalletId, network, emptySet())
            networksStatusesStore.storeActual(
                userWalletId = userWalletId,
                value = NetworkStatus(network, NetworkStatus.MissedDerivation),
            )
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch network status failure`() = runTest {
        val params = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = network,
            applyRefresh = true,
        )

        val exception = IllegalStateException()
        coEvery { userWalletsStore.getSyncStrict(key = userWalletId) } throws exception

        val actual = fetcher(params)

        coVerifyOrder {
            networksStatusesStore.refresh(userWalletId = userWalletId, network = network)
            userWalletsStore.getSyncStrict(key = userWalletId)
            networksStatusesStore.storeError(userWalletId = userWalletId, network = network)
        }

        coVerify(inverse = true) {
            walletManagersFacade.update(userWalletId = any(), network = any(), extraTokens = any())
            networksStatusesStore.storeActual(userWalletId = any(), value = any())
        }

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isEqualTo(exception)
    }

    @Test
    fun `fetch network status if applyRefresh is false`() = runTest {
        val params = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = network,
            applyRefresh = false,
        )

        val result = UpdateWalletManagerResult.MissedDerivation
        coEvery { walletManagersFacade.update(userWalletId, network, emptySet()) } returns result

        val actual = fetcher(params)

        coVerifyOrder {
            userWalletsStore.getSyncStrict(key = userWalletId)
            walletManagersFacade.update(userWalletId, network, emptySet())
            networksStatusesStore.storeActual(
                userWalletId = userWalletId,
                value = NetworkStatus(network, NetworkStatus.MissedDerivation),
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.refresh(userWalletId = any(), network = any())
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val network = MockCryptoCurrencyFactory().ethereum.network
    }
}