package com.tangem.data.networks.multi

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.networks.fetcher.CommonNetworkStatusFetcher
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.data.networks.store.setSourceAsCache
import com.tangem.data.networks.store.setSourceAsOnlyCache
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.wallets.models.UserWallet
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
internal class DefaultMultiNetworkStatusFetcherTest {

    private val networksStatusesStore: NetworksStatusesStore = mockk(relaxUnitFun = true)
    private val userWalletsStore: UserWalletsStore = mockk()
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()
    private val commonNetworkStatusFetcher: CommonNetworkStatusFetcher = mockk()

    private val fetcher = DefaultMultiNetworkStatusFetcher(
        networksStatusesStore = networksStatusesStore,
        userWalletsStore = userWalletsStore,
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        commonNetworkStatusFetcher = commonNetworkStatusFetcher,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(networksStatusesStore, userWalletsStore, cardCryptoCurrencyFactory, commonNetworkStatusFetcher)
    }

    @Test
    fun `fetch successfully for multi-currency card`() = runTest {
        // Arrange
        val networks = setOf(ethereum.network, cardano.network)
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks)
        val userWallet = MockUserWalletFactory.create()
        val cardTypesResolver = mockk<CardTypesResolver>()
        val networksCurrencies = mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        val ethereumFetcherResult = Either.Right(Unit)
        val cardanoFetcherResult = Either.Right(Unit)

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks) } returns Unit
        every { userWalletsStore.getSyncStrict(key = userWalletId) } returns userWallet

        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { userWallet.cardTypesResolver } returns cardTypesResolver
        coEvery { cardTypesResolver.isMultiwalletAllowed() } returns true

        coEvery {
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(userWallet, params.networks)
        } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
        } returns ethereumFetcherResult

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
            )
        } returns cardanoFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Right(Unit)

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            userWalletsStore.getSyncStrict(key = userWalletId)
            cardTypesResolver.isMultiwalletAllowed()
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(userWallet, params.networks)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
            cardTypesResolver.isSingleWalletWithToken()
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = any())
        }
    }

    @Test
    fun `fetch successfully for single-currency card with token`() = runTest {
        // Arrange
        val networks = setOf(ethereum.network)
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks)
        val userWallet = MockUserWalletFactory.create()
        val cardTypesResolver = mockk<CardTypesResolver>()
        val networksCurrencies = listOf(ethereum)
        val ethereumFetcherResult = Either.Right(Unit)

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks) } returns Unit
        every { userWalletsStore.getSyncStrict(key = userWalletId) } returns userWallet

        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { userWallet.cardTypesResolver } returns cardTypesResolver
        coEvery { cardTypesResolver.isMultiwalletAllowed() } returns false
        coEvery { cardTypesResolver.isSingleWalletWithToken() } returns true

        coEvery {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
        } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
        } returns ethereumFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Right(Unit)

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            userWalletsStore.getSyncStrict(key = userWalletId)
            cardTypesResolver.isMultiwalletAllowed()
            cardTypesResolver.isSingleWalletWithToken()
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(any(), any())
        }
    }

    @Test
    fun `fetch failure if one of them fails`() = runTest {
        // Arrange
        val networks = setOf(ethereum.network, cardano.network)
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks)
        val userWallet = MockUserWalletFactory.create()
        val cardTypesResolver = mockk<CardTypesResolver>()
        val networksCurrencies = mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        val ethereumFetcherResult = Either.Left(IllegalStateException())
        val cardanoFetcherResult = Either.Right(Unit)

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks) } returns Unit
        every { userWalletsStore.getSyncStrict(key = userWalletId) } returns userWallet

        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { userWallet.cardTypesResolver } returns cardTypesResolver
        coEvery { cardTypesResolver.isMultiwalletAllowed() } returns true

        coEvery {
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(userWallet, params.networks)
        } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
        } returns ethereumFetcherResult

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
            )
        } returns cardanoFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(IllegalStateException("Failed to fetch network statuses"))
        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected.leftOrNull()!!::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.leftOrNull()!!.message)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            userWalletsStore.getSyncStrict(key = userWalletId)
            cardTypesResolver.isMultiwalletAllowed()
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(userWallet, params.networks)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
            cardTypesResolver.isSingleWalletWithToken()
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = any())
        }
    }

    @Test
    fun `fetch failure if all of them fails`() = runTest {
        // Arrange
        val networks = setOf(ethereum.network, cardano.network)
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks)
        val userWallet = MockUserWalletFactory.create()
        val cardTypesResolver = mockk<CardTypesResolver>()
        val networksCurrencies = mapOf(
            ethereum.network to listOf(ethereum),
            cardano.network to listOf(cardano),
        )
        val ethereumFetcherResult = Either.Left(IllegalStateException("ethereum"))
        val cardanoFetcherResult = Either.Left(IllegalStateException("cardano"))

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks) } returns Unit
        every { userWalletsStore.getSyncStrict(key = userWalletId) } returns userWallet

        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { userWallet.cardTypesResolver } returns cardTypesResolver
        coEvery { cardTypesResolver.isMultiwalletAllowed() } returns true

        coEvery {
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(userWallet, params.networks)
        } returns networksCurrencies

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
        } returns ethereumFetcherResult

        coEvery {
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
            )
        } returns cardanoFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(IllegalStateException("Failed to fetch network statuses"))
        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected.leftOrNull()!!::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.leftOrNull()!!.message)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            userWalletsStore.getSyncStrict(key = userWalletId)
            cardTypesResolver.isMultiwalletAllowed()
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(userWallet, params.networks)
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = ethereum.network,
                networkCurrencies = setOf(ethereum),
            )
            commonNetworkStatusFetcher.fetch(
                userWalletId = params.userWalletId,
                network = cardano.network,
                networkCurrencies = setOf(cardano),
            )
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
            cardTypesResolver.isSingleWalletWithToken()
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = any())
        }
    }

    @Test
    fun `fetch failure if userWalletsStore throws exception`() = runTest {
        // Arrange
        val networks = setOf(ethereum.network, cardano.network)
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks)
        val userWalletStoreException = IllegalStateException()

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks) } returns Unit
        every { userWalletsStore.getSyncStrict(key = userWalletId) } throws userWalletStoreException
        coEvery { networksStatusesStore.setSourceAsOnlyCache(params.userWalletId, params.networks) } returns Unit

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(userWalletStoreException)

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            userWalletsStore.getSyncStrict(key = userWalletId)
            networksStatusesStore.setSourceAsOnlyCache(params.userWalletId, params.networks)
        }

        coVerify(inverse = true) {
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(any(), any())
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = any())
            commonNetworkStatusFetcher.fetch(any(), any(), any())
        }
    }

    @Test
    fun `fetch failure if card is single-currency`() = runTest {
        // Arrange
        val networks = setOf(ethereum.network, cardano.network)
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = networks)
        val userWallet = MockUserWalletFactory.create()
        val cardTypesResolver = mockk<CardTypesResolver>()

        coEvery { networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks) } returns Unit
        every { userWalletsStore.getSyncStrict(key = userWalletId) } returns userWallet

        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { userWallet.cardTypesResolver } returns cardTypesResolver
        coEvery { cardTypesResolver.isMultiwalletAllowed() } returns false
        coEvery { cardTypesResolver.isSingleWalletWithToken() } returns false

        coEvery { networksStatusesStore.setSourceAsOnlyCache(params.userWalletId, params.networks) } returns Unit

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = Either.Left(IllegalStateException("User wallet is not multi-currency"))

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected.leftOrNull()!!::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.leftOrNull()!!.message)

        coVerifyOrder {
            networksStatusesStore.setSourceAsCache(params.userWalletId, params.networks)
            userWalletsStore.getSyncStrict(key = userWalletId)
            cardTypesResolver.isMultiwalletAllowed()
            cardTypesResolver.isSingleWalletWithToken()
            networksStatusesStore.setSourceAsOnlyCache(params.userWalletId, params.networks)
        }

        coVerify(inverse = true) {
            networksStatusesStore.setSourceAsOnlyCache(userWalletId = any(), networks = any())
            cardCryptoCurrencyFactory.createCurrenciesForMultiCurrencyCard(any(), any())
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(scanResponse = any())
            commonNetworkStatusFetcher.fetch(any(), any(), any())
        }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
        val ethereum = cryptoCurrencyFactory.ethereum
        val cardano = cryptoCurrencyFactory.cardano
    }
}