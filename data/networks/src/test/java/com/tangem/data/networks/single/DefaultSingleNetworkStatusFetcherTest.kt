package com.tangem.data.networks.single

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.fetcher.CommonNetworkStatusFetcher
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSingleNetworkStatusFetcherTest {

    private val commonNetworkStatusFetcher: CommonNetworkStatusFetcher = mockk()
    private val networksStatusesStore: NetworksStatusesStore = mockk(relaxUnitFun = true)
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()

    private val fetcher = DefaultSingleNetworkStatusFetcher(
        commonNetworkStatusFetcher = commonNetworkStatusFetcher,
        networksStatusesStore = networksStatusesStore,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(commonNetworkStatusFetcher, networksStatusesStore, cardCryptoCurrencyFactory)
    }

    @Test
    fun `fetch successfully`() = runTest {
        // Arrange
        val params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = ethereum.network)
        val networkCurrencies = listOf(ethereum)
        val commonFetcherResult = Either.Right(Unit)

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.network) } returns Unit
        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.network) } returns networkCurrencies
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = params.network,
                networkCurrencies = networkCurrencies.toSet(),
            )
        } returns commonFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = commonFetcherResult

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.network)
            cardCryptoCurrencyFactory.create(params.userWalletId, params.network)
            commonNetworkStatusFetcher.fetch(params.userWalletId, params.network, setOf(ethereum))
        }
    }

    @Test
    fun `fetch failure if cardCryptoCurrencyFactory throws exception`() = runTest {
        // Arrange
        val params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = ethereum.network)
        val factoryException = IllegalStateException()

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.network) } returns Unit
        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.network) } throws factoryException

        // Act
        val actual = fetcher(params)

        // Arrange
        val expected = Either.Left(factoryException)
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(userWalletId = params.userWalletId, network = params.network)
            cardCryptoCurrencyFactory.create(userWalletId = params.userWalletId, network = params.network)
        }

        coVerify(inverse = true) {
            commonNetworkStatusFetcher.fetch(userWalletId = any(), network = any(), networkCurrencies = any())
        }
    }

    @Test
    fun `fetch failure if commonNetworkStatusFetcher returns exception`() = runTest {
        // Arrange
        val params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = ethereum.network)
        val networkCurrencies = listOf(ethereum)
        val commonFetcherResult = IllegalStateException().left()

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.network) } returns Unit
        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.network) } returns networkCurrencies
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = params.network,
                networkCurrencies = networkCurrencies.toSet(),
            )
        } returns commonFetcherResult

        // Act
        val actual = fetcher(params)

        // Arrange
        val expected = commonFetcherResult

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(userWalletId = params.userWalletId, network = params.network)
            cardCryptoCurrencyFactory.create(userWalletId = params.userWalletId, network = params.network)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = params.network,
                networkCurrencies = setOf(ethereum),
            )
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
        val ethereum = MockCryptoCurrencyFactory().ethereum
    }
}