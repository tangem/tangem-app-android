package com.tangem.data.networks.multi

import arrow.core.Either
import arrow.core.left
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.dynamicaddresses.DynamicAddressesInitializer
import com.tangem.domain.common.tokens.CardCryptoCurrencyFactory
import com.tangem.data.networks.fetcher.CommonNetworkStatusFetcher
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.test.core.assertEither
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
internal class DefaultMultiNetworkStatusFetcherTest {

    private val networksStatusesStore: NetworksStatusesStore = mockk(relaxUnitFun = true)
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()
    private val commonNetworkStatusFetcher: CommonNetworkStatusFetcher = mockk()
    private val dynamicAddressesInitializer: DynamicAddressesInitializer = mockk()

    private val fetcher = DefaultMultiNetworkStatusFetcher(
        networksStatusesStore = networksStatusesStore,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        commonNetworkStatusFetcher = commonNetworkStatusFetcher,
        dynamicAddressesInitializer = dynamicAddressesInitializer,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(networksStatusesStore, cardCryptoCurrencyFactory, commonNetworkStatusFetcher, dynamicAddressesInitializer)
        // No dynamic addresses restore by default
        coEvery { dynamicAddressesInitializer.getXpubs(any(), any()) } returns emptyMap()
    }

    @Test
    fun `fetch successfully`() = runTest {
        // Arrange
        val params = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = setOf(ethereum.network, cardano.network),
        )

        val networksCurrencies = mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        val ethereumFetcherResult = Either.Right(Unit)
        val cardanoFetcherResult = Either.Right(Unit)

        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.networks) } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
        } returns ethereumFetcherResult

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        } returns cardanoFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Right(Unit)
        assertEither(actual, expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            cardCryptoCurrencyFactory.create(params.userWalletId, params.networks)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
        }
    }

    @Test
    fun `fetch failure if one of them fails`() = runTest {
        // Arrange
        val params = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = setOf(ethereum.network, cardano.network),
        )

        val networksCurrencies = mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        val ethereumFetcherResult = Either.Left(IllegalStateException())
        val cardanoFetcherResult = Either.Right(Unit)

        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.networks) } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
        } returns ethereumFetcherResult

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        } returns cardanoFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(IllegalStateException("Failed to fetch network statuses"))
        assertEither(actual, expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            cardCryptoCurrencyFactory.create(params.userWalletId, params.networks)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
        }
    }

    @Test
    fun `fetch failure if all of them fails`() = runTest {
        // Arrange
        val params = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = setOf(ethereum.network, cardano.network),
        )

        val networksCurrencies = mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        val ethereumFetcherResult = Either.Left(IllegalStateException("ethereum"))
        val cardanoFetcherResult = Either.Left(IllegalStateException("cardano"))

        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.networks) } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
        } returns ethereumFetcherResult

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        } returns cardanoFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(IllegalStateException("Failed to fetch network statuses"))
        assertEither(actual, expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            cardCryptoCurrencyFactory.create(params.userWalletId, params.networks)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
        }
    }

    @Test
    fun `fetch failure if cardCryptoCurrencyFactory throws exception`() = runTest {
        // Arrange
        val params = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = setOf(ethereum.network, cardano.network),
        )

        val factoryException = IllegalStateException()

        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.networks) } throws factoryException

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = factoryException.left()
        assertEither(actual, expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            cardCryptoCurrencyFactory.create(params.userWalletId, params.networks)
            networksStatusesStore.setSourceAsOnlyCache(params.userWalletId, params.networks)
        }

        coVerify(inverse = true) { commonNetworkStatusFetcher.fetch(any(), any(), any(), any()) }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
        val ethereum = cryptoCurrencyFactory.ethereum
        val cardano = cryptoCurrencyFactory.cardano
    }
}