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

    @Test
    fun `fetch passes xpub to correct network and null to others`() = runTest {
        // Arrange
        val xpub = "xpub_test_eth"
        val params = setupTwoNetworkParams()
        coEvery { dynamicAddressesInitializer.getXpubs(params.userWalletId, params.networks) } returns mapOf(ethereum.network to xpub)

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = xpub,
            )
        } returns Either.Right(Unit)
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        } returns Either.Right(Unit)

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Right(Unit)
        assertEither(actual, expected)
        coVerify(exactly = 1) {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = xpub,
            )
        }
        coVerify(exactly = 1) {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        }
    }

    @Test
    fun `fetch continues with null xpub for all networks if getXpubs throws`() = runTest {
        // Arrange
        val params = setupTwoNetworkParams()
        coEvery {
            dynamicAddressesInitializer.getXpubs(params.userWalletId, params.networks)
        } throws RuntimeException("XPUB derivation failed")

        val fetchResult = Either.Right(Unit)
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
        } returns fetchResult
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        } returns fetchResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Right(Unit)
        assertEither(actual, expected)
        coVerify(exactly = 1) {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = null,
            )
        }
        coVerify(exactly = 1) {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        }
        // getXpubs failure must not degrade network status to OnlyCache
        coVerify(inverse = true) { networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any()) }
    }

    @Test
    fun `fetch passes xpubs to all networks returned by getXpubs`() = runTest {
        // Arrange
        val ethXpub = "xpub_eth"
        val adaXpub = "xpub_ada"
        val params = setupTwoNetworkParams()
        coEvery {
            dynamicAddressesInitializer.getXpubs(params.userWalletId, params.networks)
        } returns mapOf(ethereum.network to ethXpub, cardano.network to adaXpub)

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = ethXpub,
            )
        } returns Either.Right(Unit)
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = adaXpub,
            )
        } returns Either.Right(Unit)

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Right(Unit)
        assertEither(actual, expected)
        coVerify(exactly = 1) {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = ethXpub,
            )
        }
        coVerify(exactly = 1) {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = adaXpub,
            )
        }
    }

    @Test
    fun `fetch calls getXpubs with exactly the networks from params`() = runTest {
        // Arrange
        val params = setupTwoNetworkParams()
        coEvery {
            commonNetworkStatusFetcher.fetch(userWalletId = any(), network = any(), networkCurrencies = any(), xpub = any())
        } returns Either.Right(Unit)

        // Act
        fetcher(params)

        // Assert
        coVerify(exactly = 1) { dynamicAddressesInitializer.getXpubs(params.userWalletId, params.networks) }
    }

    @Test
    fun `fetch failure if network fetch fails when xpub is provided`() = runTest {
        // Arrange
        val xpub = "xpub_eth"
        val params = setupTwoNetworkParams()
        coEvery {
            dynamicAddressesInitializer.getXpubs(params.userWalletId, params.networks)
        } returns mapOf(ethereum.network to xpub)

        val fetchFailure = Either.Left(IllegalStateException())
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
                xpub = xpub,
            )
        } returns fetchFailure
        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
                xpub = null,
            )
        } returns Either.Right(Unit)

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(IllegalStateException("Failed to fetch network statuses"))
        assertEither(actual, expected)
    }

    private fun setupTwoNetworkParams(): MultiNetworkStatusFetcher.Params {
        val params = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = setOf(ethereum.network, cardano.network),
        )
        coEvery { cardCryptoCurrencyFactory.create(params.userWalletId, params.networks) } returns mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        return params
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
        val ethereum = cryptoCurrencyFactory.ethereum
        val cardano = cryptoCurrencyFactory.cardano
    }
}