package com.tangem.data.tokens

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMultiWalletCryptoCurrenciesProducerTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWallet.walletId)
    private val userWalletsStore: UserWalletsStore = mockk(relaxUnitFun = true)
    private val userTokensResponseStore: UserTokensResponseStore = mockk(relaxUnitFun = true)
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk()

    private val producer = DefaultMultiWalletCryptoCurrenciesProducer(
        params = params,
        userWalletsStore = userWalletsStore,
        userTokensResponseStore = userTokensResponseStore,
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsStore, userTokensResponseStore, responseCryptoCurrenciesFactory)
    }

    @Test
    fun `flow is mapped for user wallet id from params`() = runTest {
        // Arrange
        val userTokensResponseFlow = flowOf<UserTokensResponse?>(null)

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns userWallet
        every { userTokensResponseStore.get(params.userWalletId) } returns userTokensResponseFlow

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = emptySet<CryptoCurrency>()

        Truth.assertThat(actual.size).isEqualTo(1)
        Truth.assertThat(actual.first()).isEqualTo(expected)

        verifyOrder {
            userWalletsStore.getSyncStrict(params.userWalletId)
            userTokensResponseStore.get(params.userWalletId)
        }

        verify(inverse = true) {
            responseCryptoCurrenciesFactory.createCurrencies(response = any(), userWallet = any())
        }
    }

    @Test
    fun `flow will updated if UserTokensResponse is updated`() = runTest {
        // Arrange
        val userTokensResponseFlow = MutableSharedFlow<UserTokensResponse>(replay = 2)

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.TOKEN,
            sort = UserTokensResponse.SortType.MARKETCAP,
            tokens = emptyList(),
        )
        val cryptoCurrencies = emptySet<CryptoCurrency>()

        val updatedUserTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.TOKEN,
            sort = UserTokensResponse.SortType.MARKETCAP,
            tokens = listOf(
                UserTokensResponse.Token(
                    id = null,
                    networkId = "bitcoin",
                    derivationPath = null,
                    name = "Bitcoin",
                    symbol = "BTC",
                    decimals = 8,
                    contractAddress = null,
                    addresses = listOf(),
                ),
            ),
        )
        val updatedCryptoCurrencies = setOf(
            cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin),
        )

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns userWallet
        every { userTokensResponseStore.get(params.userWalletId) } returns userTokensResponseFlow

        every {
            responseCryptoCurrenciesFactory.createCurrencies(
                response = userTokensResponse,
                userWallet = userWallet,
            )
        } returns cryptoCurrencies.toList()

        every {
            responseCryptoCurrenciesFactory.createCurrencies(
                response = updatedUserTokensResponse,
                userWallet = userWallet,
            )
        } returns updatedCryptoCurrencies.toList()

        val producerFlow = producer.produce()

        // Act 1 (first emit)
        userTokensResponseFlow.emit(userTokensResponse)

        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected1 = cryptoCurrencies

        Truth.assertThat(actual1.size).isEqualTo(1)
        Truth.assertThat(actual1.first()).isEqualTo(expected1)

        verifyOrder {
            userWalletsStore.getSyncStrict(params.userWalletId)
            userTokensResponseStore.get(params.userWalletId)
            responseCryptoCurrenciesFactory.createCurrencies(
                response = userTokensResponse,
                userWallet = userWallet,
            )
        }

        // Act 2 (second emit)
        userTokensResponseFlow.emit(updatedUserTokensResponse)

        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected2 = listOf(cryptoCurrencies, updatedCryptoCurrencies)

        Truth.assertThat(actual2.size).isEqualTo(2)
        Truth.assertThat(actual2).isEqualTo(expected2)

        verifyOrder {
            responseCryptoCurrenciesFactory.createCurrencies(
                response = updatedUserTokensResponse,
                userWallet = userWallet,
            )
        }
    }

    @Test
    fun `flow is filtered the same status`() = runTest {
        // Arrange
        val userTokensResponseFlow = MutableSharedFlow<UserTokensResponse>(replay = 2)

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.TOKEN,
            sort = UserTokensResponse.SortType.MARKETCAP,
            tokens = emptyList(),
        )

        val cryptoCurrencies = emptySet<CryptoCurrency>()

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns userWallet
        every { userTokensResponseStore.get(params.userWalletId) } returns userTokensResponseFlow

        every {
            responseCryptoCurrenciesFactory.createCurrencies(
                response = userTokensResponse,
                userWallet = userWallet,
            )
        } returns cryptoCurrencies.toList()

        val producerFlow = producer.produce()

        // Act 1 (first emit)
        userTokensResponseFlow.emit(userTokensResponse)

        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected1 = cryptoCurrencies

        Truth.assertThat(actual1.size).isEqualTo(1)
        Truth.assertThat(actual1.first()).isEqualTo(expected1)

        verifyOrder {
            userWalletsStore.getSyncStrict(params.userWalletId)
            userTokensResponseStore.get(params.userWalletId)
            responseCryptoCurrenciesFactory.createCurrencies(
                response = userTokensResponse,
                userWallet = userWallet,
            )
        }

        // Act 2 (second emit)
        userTokensResponseFlow.emit(userTokensResponse)

        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected2 = expected1
        Truth.assertThat(actual2.size).isEqualTo(1)
        Truth.assertThat(actual2.first()).isEqualTo(expected2)
    }

    @Test
    fun `flow throws exception`() = runTest {
        // Arrange
        val exception = IllegalStateException()

        val userTokensResponse = UserTokensResponse(
            group = UserTokensResponse.GroupType.TOKEN,
            sort = UserTokensResponse.SortType.MARKETCAP,
            tokens = emptyList(),
        )

        val cryptoCurrencies = emptySet<CryptoCurrency>()

        val innerFlow = MutableStateFlow(value = false)
        val userTokensResponseFlow = flow {
            if (innerFlow.value) {
                emit(userTokensResponse)
            } else {
                throw exception
            }
        }
            .buffer(capacity = 5)

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns userWallet
        every { userTokensResponseStore.get(params.userWalletId) } returns userTokensResponseFlow

        every {
            responseCryptoCurrenciesFactory.createCurrencies(
                response = userTokensResponse,
                userWallet = userWallet,
            )
        } returns cryptoCurrencies.toList()

        val producerFlow = producer.produceWithFallback()

        // Act 1 (fallback)
        val actual1 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected1 = producer.fallback
        Truth.assertThat(actual1.size).isEqualTo(1)
        Truth.assertThat(actual1.first()).isEqualTo(expected1)

        verifyOrder {
            userWalletsStore.getSyncStrict(params.userWalletId)
            userTokensResponseStore.get(params.userWalletId)
        }

        // Act 2 (emit)
        innerFlow.emit(value = true)
        val actual2 = getEmittedValues(flow = producerFlow)

        // Assert
        val expected2 = cryptoCurrencies
        Truth.assertThat(actual2.size).isEqualTo(1)
        Truth.assertThat(actual2.first()).isEqualTo(expected2)

        verifyOrder {
            responseCryptoCurrenciesFactory.createCurrencies(
                response = userTokensResponse,
                userWallet = userWallet,
            )
        }
    }

    @Test
    fun `flow is empty if store returns empty flow`() = runTest {
        // Arrange
        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns userWallet
        every { userTokensResponseStore.get(params.userWalletId) } returns emptyFlow()

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = producer.fallback
        Truth.assertThat(actual.size).isEqualTo(1)
        Truth.assertThat(actual.first()).isEqualTo(expected)

        verifyOrder {
            userWalletsStore.getSyncStrict(params.userWalletId)
            userTokensResponseStore.get(params.userWalletId)
        }

        verify(inverse = true) {
            responseCryptoCurrenciesFactory.createCurrencies(response = any(), userWallet = any())
        }
    }

    @Test
    fun `produce throws exception if UserWallet isn't multi-currency wallet`() = runTest {
        // Arrange
        val mockUserWallet = mockk<UserWallet> {
            every { isMultiCurrency } returns false
        }

        every { userWalletsStore.getSyncStrict(params.userWalletId) } returns mockUserWallet

        // Act
        val actual = runCatching { producer.produce() }.exceptionOrNull()

        // Assert
        val expected = IllegalStateException(
            "${DefaultMultiWalletCryptoCurrenciesProducer::class.simpleName} supports only multi-currency wallet",
        )

        Truth.assertThat(actual).isInstanceOf(expected::class.java)
        Truth.assertThat(actual).hasMessageThat().isEqualTo(expected.message)

        verifyOrder { userWalletsStore.getSyncStrict(params.userWalletId) }

        verify(inverse = true) {
            userTokensResponseStore.get(any())
            responseCryptoCurrenciesFactory.createCurrencies(response = any(), userWallet = any())
        }
    }

    private companion object {

        val scanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(2),
            derivedKeys = emptyMap(),
        )

        val userWallet = MockUserWalletFactory.create(scanResponse = scanResponse)
    }
}