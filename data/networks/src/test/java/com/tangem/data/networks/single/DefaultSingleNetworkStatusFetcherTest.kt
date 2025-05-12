package com.tangem.data.networks.single

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStoreV2
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

    private val walletManagersFacade: WalletManagersFacade = mockk(relaxUnitFun = true)
    private val networksStatusesStore: NetworksStatusesStoreV2 = mockk(relaxUnitFun = true)
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()

    private val fetcher = DefaultSingleNetworkStatusFetcher(
        walletManagersFacade = walletManagersFacade,
        networksStatusesStore = networksStatusesStore,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `fetch network status successfully`() = runTest {
        val params = createParams()

        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.network) } returns listOf(ethereum)

        val result = UpdateWalletManagerResult.MissedDerivation
        coEvery { walletManagersFacade.update(params.userWalletId, params.network, emptySet()) } returns result

        val actual = fetcher(params)

        coVerifyOrder {
            networksStatusesStore.refresh(params.userWalletId, params.network)
            cardCryptoCurrencyFactory.create(params.userWalletId, params.network)
            walletManagersFacade.update(params.userWalletId, params.network, emptySet())
            networksStatusesStore.storeSuccess(
                userWalletId = params.userWalletId,
                value = NetworkStatus(params.network, NetworkStatus.MissedDerivation),
            )
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch network status failure`() = runTest {
        val params = createParams()

        val exception = IllegalStateException()
        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.network) } throws exception

        val actual = fetcher(params)

        coVerifyOrder {
            networksStatusesStore.refresh(userWalletId = params.userWalletId, network = params.network)
            cardCryptoCurrencyFactory.create(userWalletId = params.userWalletId, network = params.network)
            networksStatusesStore.storeError(userWalletId = params.userWalletId, network = params.network)
        }

        coVerify(inverse = true) {
            walletManagersFacade.update(userWalletId = any(), network = any(), extraTokens = any())
            networksStatusesStore.storeSuccess(userWalletId = any(), value = any())
        }

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isEqualTo(exception)
    }

    private fun createParams(): SingleNetworkStatusFetcher.Params {
        return SingleNetworkStatusFetcher.Params.Simple(
            userWalletId = UserWalletId("011"),
            network = ethereum.network,
        )
    }

    private companion object {

        val ethereum = MockCryptoCurrencyFactory().ethereum
    }
}