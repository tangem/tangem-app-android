package com.tangem.features.earn.lending.impl.supplier

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.features.earn.lending.impl.provider.FakeLendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultLendingProducerTest {

    private val userWalletId = UserWalletId("011")
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val weth = currencies.createToken(Blockchain.Ethereum, contractAddress = WETH, symbol = "WETH")
    private val asset = LendingAsset(currency = eth, wrappedToken = weth)

    private val accountListSupplier: SingleAccountListSupplier = mockk()
    private val networkStatusSupplier: SingleNetworkStatusSupplier = mockk()
    private val quoteStatusSupplier: SingleQuoteStatusSupplier = mockk()

    private val accountCurrencies = MutableStateFlow<List<CryptoCurrency>>(listOf(eth))
    private val networkStatus = MutableStateFlow(verified())

    @BeforeEach
    fun setUp() {
        clearMocks(accountListSupplier, networkStatusSupplier, quoteStatusSupplier)
        accountCurrencies.value = listOf(eth)
        networkStatus.value = verified()

        every { accountListSupplier(userWalletId) } returns accountLists(accountCurrencies)
        every {
            networkStatusSupplier(SingleNetworkStatusProducer.Params(userWalletId, eth.network))
        } returns networkStatus
        every {
            quoteStatusSupplier(SingleQuoteStatusProducer.Params(eth.id.rawCurrencyId!!))
        } returns MutableStateFlow(quote())
    }

    @Test
    fun `GIVEN no currency the provider covers WHEN produce THEN no assets`() = runTest {
        // Arrange
        val producer = createProducer(FakeLendingProvider())

        // Act
        val actual = producer.produce().first()

        // Assert
        assertThat(actual).isEqualTo(LendingSnapshot.NoAssets)
    }

    @Test
    fun `GIVEN ETH in the account WHEN produce THEN asset state by the coin id with network data`() = runTest {
        // Arrange
        val provider = FakeLendingProvider(assets = mapOf(eth.id to asset))
        val producer = createProducer(provider)

        // Act
        val actual = producer.produce().first()

        // Assert
        val expected = LendingSnapshot(
            assets = mapOf(
                eth.id to LendingAssetState(
                    asset = asset,
                    address = ADDRESS,
                    balance = BigDecimal("2"),
                    fiatRate = BigDecimal("3000"),
                    market = provider.marketFlow.value,
                    hasPendingTx = false,
                    isWrappedTokenInWallet = false,
                    source = StatusSource.CACHE,
                ),
            ),
            source = StatusSource.CACHE,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN outgoing pending ETH transaction WHEN produce THEN pending flag is set`() = runTest {
        // Arrange
        networkStatus.value = verified(pending = setOf(txInfo(isOutgoing = true)))
        val producer = createProducer(FakeLendingProvider(assets = mapOf(eth.id to asset)))

        // Act
        val actual = producer.produce().first()

        // Assert
        assertThat(actual.assets.getValue(eth.id).hasPendingTx).isTrue()
    }

    @Test
    fun `GIVEN incoming pending ETH transaction WHEN produce THEN pending flag is not set`() = runTest {
        // Arrange
        networkStatus.value = verified(pending = setOf(txInfo(isOutgoing = false)))
        val producer = createProducer(FakeLendingProvider(assets = mapOf(eth.id to asset)))

        // Act
        val actual = producer.produce().first()

        // Assert
        assertThat(actual.assets.getValue(eth.id).hasPendingTx).isFalse()
    }

    @Test
    fun `GIVEN actual network and failed provider read WHEN produce THEN source is the weakest`() = runTest {
        // Arrange
        networkStatus.value = verified(source = StatusSource.ACTUAL)
        val provider = FakeLendingProvider(assets = mapOf(eth.id to asset))
        provider.marketFlow.value = FakeLendingProvider.loadingMarket(source = StatusSource.ONLY_CACHE)
        val producer = createProducer(provider)

        // Act
        val actual = producer.produce().first()

        // Assert
        assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
        assertThat(actual.assets.getValue(eth.id).source).isEqualTo(StatusSource.ONLY_CACHE)
    }

    @Test
    fun `GIVEN WETH token in the account WHEN produce THEN wrapped token is marked as in wallet`() = runTest {
        // Arrange
        accountCurrencies.value = listOf(eth, weth)
        val producer = createProducer(FakeLendingProvider(assets = mapOf(eth.id to asset)))

        // Act
        val actual = producer.produce().first()

        // Assert
        assertThat(actual.assets.getValue(eth.id).isWrappedTokenInWallet).isTrue()
    }

    @Test
    fun `GIVEN account list fails WHEN produce with fallback THEN failed snapshot`() = runTest {
        // Arrange
        every { accountListSupplier(userWalletId) } returns flow { throw IllegalStateException("accounts") }
        val producer = createProducer(FakeLendingProvider(assets = mapOf(eth.id to asset)))

        // Act
        val actual = producer.produceWithFallback().first()

        // Assert
        assertThat(actual).isEqualTo(LendingSnapshot.Failed)
    }

    @Test
    fun `GIVEN two wallets WHEN supplier invoked THEN one producer per wallet`() = runTest {
        // Arrange
        val otherWalletId = UserWalletId("022")
        every { accountListSupplier(otherWalletId) } returns accountLists(accountCurrencies)
        val created = mutableListOf<UserWalletId>()
        val factory = object : LendingProducer.Factory {
            override fun create(params: LendingProducer.Params): LendingProducer {
                created += params.userWalletId
                return createProducer(FakeLendingProvider(), params.userWalletId)
            }
        }
        val supplier = LendingSupplier(factory = factory, keyCreator = { "lending_${it.userWalletId.stringValue}" })

        // Act
        supplier(userWalletId)
        supplier(otherWalletId)
        supplier(userWalletId)

        // Assert
        assertThat(created).containsExactly(userWalletId, otherWalletId).inOrder()
    }

    private fun TestScope.createProducer(
        provider: FakeLendingProvider,
        walletId: UserWalletId = userWalletId,
    ) = DefaultLendingProducer(
        params = LendingProducer.Params(walletId),
        flowProducerTools = TestFlowProducerTools(backgroundScope, StandardTestDispatcher(testScheduler)),
        accountListSupplier = accountListSupplier,
        networkStatusSupplier = networkStatusSupplier,
        quoteStatusSupplier = quoteStatusSupplier,
        provider = provider,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )

    private fun accountLists(currencies: MutableStateFlow<List<CryptoCurrency>>): Flow<AccountList> = flow {
        currencies.collect { list ->
            emit(mockk<AccountList> { every { flattenCurrencies() } returns list })
        }
    }

    private fun verified(
        pending: Set<TxInfo> = emptySet(),
        source: StatusSource = StatusSource.CACHE,
    ) = NetworkStatus(
        network = eth.network,
        value = NetworkStatus.Verified(
            address = NetworkAddress.Single(NetworkAddress.Address(ADDRESS, NetworkAddress.Address.Type.Primary)),
            amounts = mapOf(eth.id to NetworkStatus.Amount.Loaded(BigDecimal("2"))),
            pendingTransactions = mapOf(eth.id to pending),
            yieldSupplyStatuses = emptyMap(),
            source = source,
        ),
    )

    private fun txInfo(isOutgoing: Boolean): TxInfo = mockk { every { this@mockk.isOutgoing } returns isOutgoing }

    private fun quote() = QuoteStatus(
        rawCurrencyId = eth.id.rawCurrencyId!!,
        value = mockk<QuoteStatus.Data> { every { fiatRate } returns BigDecimal("3000") },
    )

    private companion object {
        const val ADDRESS = "0x1111111111111111111111111111111111111111"
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    }
}