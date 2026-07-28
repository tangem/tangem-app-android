package com.tangem.features.commonfeatures.impl.choosetoken.predefined

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.markets.RawMarketToken
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.PredefinedTokenToAdd
import com.tangem.features.commonfeatures.impl.choosetoken.AddToPortfolioRoute
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokenItemUM
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokensUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredefinedTokensBlockDelegateTest {

    private val addToPortfolioManager: AddToPortfolioManager = mockk(relaxUnitFun = true)
    private val addToPortfolioSlot: SlotNavigation<AddToPortfolioRoute> = mockk(relaxUnitFun = true)
    private val account: AccountStatus = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(addToPortfolioManager, addToPortfolioSlot)
    }

    @Test
    fun `GIVEN single token AND blank query WHEN state emitted THEN token mapped to item`() = runTest {
        // Arrange
        val token = createPredefinedToken(
            id = "bitcoin",
            symbol = "BTC",
            networkId = ETHEREUM_NETWORK_ID,
            iconUrl = "https://icon/btc.png",
        )
        val delegate = createDelegate(predefinedTokens = MutableStateFlow(listOf(token)))

        // Act
        val actual = lastState(delegate)

        // Assert
        val actualItem = requireNotNull(actual).items.single()
        val expected = PredefinedTokenItemUM(
            id = "bitcoin_$ETHEREUM_NETWORK_ID",
            symbol = "BTC",
            networkName = TextReference.Str("Ethereum"),
            networkId = ETHEREUM_NETWORK_ID,
            iconUrl = "https://icon/btc.png",
            onAddClick = actualItem.onAddClick,
        )
        assertThat(actualItem).isEqualTo(expected)
    }

    @Test
    fun `GIVEN same token on two networks WHEN state emitted THEN item ids are unique`() = runTest {
        // Arrange
        val tokens = listOf(
            createPredefinedToken(id = "usd-coin", symbol = "USDC", networkId = "ethereum"),
            createPredefinedToken(id = "usd-coin", symbol = "USDC", networkId = "polygon-pos"),
        )
        val delegate = createDelegate(predefinedTokens = MutableStateFlow(tokens))

        // Act
        val actualIds = requireNotNull(lastState(delegate)).items.map { it.id }

        // Assert
        assertThat(actualIds).containsExactly("usd-coin_ethereum", "usd-coin_polygon-pos").inOrder()
    }

    @Test
    fun `GIVEN token network without decimals WHEN state emitted THEN token dropped`() = runTest {
        // Arrange
        val valid = createPredefinedToken(id = "bitcoin", networkId = "ethereum")
        val noDecimals = PredefinedTokenToAdd(
            token = RawMarketToken(id = CryptoCurrency.RawID("ghost"), name = "Ghost", symbol = "GHOST"),
            network = TokenMarketInfo.Network(
                networkId = "ethereum",
                isExchangeable = false,
                contractAddress = null,
                decimalCount = null,
            ),
            iconUrl = null,
        )
        val delegate = createDelegate(predefinedTokens = MutableStateFlow(listOf(valid, noDecimals)))

        // Act
        val actual = lastState(delegate)

        // Assert
        assertThat(actual?.items?.map { it.id }).containsExactly("bitcoin_ethereum")
    }

    @Test
    fun `GIVEN empty predefined list WHEN state emitted THEN emits null`() = runTest {
        // Arrange
        val delegate = createDelegate(predefinedTokens = MutableStateFlow(emptyList()))

        // Act
        val actual = lastState(delegate)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN predefined token already in portfolio WHEN state emitted THEN it is excluded`() = runTest {
        // Arrange
        val tokens = listOf(
            createPredefinedToken(id = "usd-coin", symbol = "USDC", networkId = ETHEREUM_NETWORK_ID),
            createPredefinedToken(id = "tether", symbol = "USDT", networkId = ETHEREUM_NETWORK_ID),
        )
        val delegate = createDelegate(
            predefinedTokens = MutableStateFlow(tokens),
            portfolioTokenKeys = MutableStateFlow(setOf("usd-coin" to ETHEREUM_NETWORK_ID)),
        )

        // Act
        val actual = lastState(delegate)

        // Assert — usd-coin is already in the portfolio, so only tether stays in "Other eligible tokens"
        assertThat(actual?.items?.map { it.id }).containsExactly("tether_$ETHEREUM_NETWORK_ID")
    }

    @Test
    fun `GIVEN all predefined tokens already in portfolio WHEN state emitted THEN emits null`() = runTest {
        // Arrange
        val token = createPredefinedToken(id = "usd-coin", symbol = "USDC", networkId = ETHEREUM_NETWORK_ID)
        val delegate = createDelegate(
            predefinedTokens = MutableStateFlow(listOf(token)),
            portfolioTokenKeys = MutableStateFlow(setOf("usd-coin" to ETHEREUM_NETWORK_ID)),
        )

        // Act
        val actual = lastState(delegate)

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun filter(model: FilterModel) = runTest {
        // Arrange
        val tokens = listOf(
            createPredefinedToken(id = "bitcoin", name = "Bitcoin", symbol = "BTC"),
            createPredefinedToken(id = "ethereum", name = "Ethereum", symbol = "ETH"),
            createPredefinedToken(id = "solana", name = "Solana", symbol = "SOL"),
        )
        val delegate = createDelegate(
            predefinedTokens = MutableStateFlow(tokens),
            searchQueryState = MutableStateFlow(SearchQuery(model.query)),
        )

        // Act
        val actual = lastState(delegate)

        // Assert
        val expectedIds = model.expectedIds
        if (expectedIds == null) {
            assertThat(actual).isNull()
        } else {
            assertThat(actual?.items?.map { it.id }).containsExactlyElementsIn(expectedIds).inOrder()
        }
    }

    @Test
    fun `GIVEN token WHEN onAddClick invoked THEN manager updated AND slot activated`() = runTest {
        // Arrange
        val rawToken = RawMarketToken(id = CryptoCurrency.RawID("bitcoin"), name = "Bitcoin", symbol = "BTC")
        val network = network(ETHEREUM_NETWORK_ID)
        val token = PredefinedTokenToAdd(token = rawToken, network = network, iconUrl = null)
        val delegate = createDelegate(predefinedTokens = MutableStateFlow(listOf(token)))
        val item = requireNotNull(lastState(delegate)).items.single()

        // Act
        item.onAddClick()

        // Assert
        val transformer: CapturingSlot<(AddToPortfolioRoute?) -> AddToPortfolioRoute?> = slot()
        verify(exactly = 1) { addToPortfolioManager.setTokenParams(rawToken) }
        verify(exactly = 1) { addToPortfolioManager.setTokenNetworks(listOf(network)) }
        verify(exactly = 1) { addToPortfolioSlot.navigate(capture(transformer), any()) }
        assertThat(transformer.captured.invoke(null)).isEqualTo(AddToPortfolioRoute)
    }

    @Test
    fun `GIVEN predefined tokens WHEN emitted THEN tokenFilter matches only those tokens`() = runTest {
        // Arrange
        val tokenFilter = MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean>({ _, _ -> true })
        val predefinedTokens = MutableStateFlow<List<PredefinedTokenToAdd>>(emptyList())
        createDelegate(predefinedTokens = predefinedTokens, tokenFilter = tokenFilter)

        // Act
        predefinedTokens.value = listOf(
            createPredefinedToken(id = "usd-coin", networkId = "ethereum"),
            createPredefinedToken(id = "tether", networkId = "polygon-pos"),
        )
        advanceUntilIdle()

        // Assert
        val predicate = tokenFilter.value
        assertThat(predicate(account, currency(rawId = "usd-coin", networkId = "ethereum"))).isTrue()
        assertThat(predicate(account, currency(rawId = "tether", networkId = "polygon-pos"))).isTrue()
        // same network, different token → excluded
        assertThat(predicate(account, currency(rawId = "shiba-inu", networkId = "ethereum"))).isFalse()
        // same token, different network → excluded
        assertThat(predicate(account, currency(rawId = "usd-coin", networkId = "solana"))).isFalse()
    }

    @Test
    fun `GIVEN only invalid predefined tokens WHEN emitted THEN tokenFilter stays pass-through`() = runTest {
        // Arrange
        val tokenFilter = MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean>({ _, _ -> false })
        val predefinedTokens = MutableStateFlow<List<PredefinedTokenToAdd>>(emptyList())
        createDelegate(predefinedTokens = predefinedTokens, tokenFilter = tokenFilter)

        // Act — a token whose network has no decimals is invalid (not addable), so must not constrain the list
        predefinedTokens.value = listOf(
            PredefinedTokenToAdd(
                token = RawMarketToken(id = CryptoCurrency.RawID("usd-coin"), name = "USD Coin", symbol = "USDC"),
                network = TokenMarketInfo.Network(
                    networkId = "ethereum",
                    isExchangeable = false,
                    contractAddress = null,
                    decimalCount = null,
                ),
                iconUrl = null,
            ),
        )
        advanceUntilIdle()

        // Assert — no valid predefined tokens → filter shows everything
        assertThat(tokenFilter.value(account, currency(rawId = "shiba-inu", networkId = "ethereum"))).isTrue()
    }

    // region Helpers

    private fun TestScope.lastState(delegate: PredefinedTokensBlockDelegate): PredefinedTokensUM? {
        val emittedValues = getEmittedValues(delegate.stateFlow)
        advanceUntilIdle()
        return emittedValues.last()
    }

    private fun TestScope.createDelegate(
        predefinedTokens: MutableStateFlow<List<PredefinedTokenToAdd>>,
        searchQueryState: MutableStateFlow<SearchQuery> = MutableStateFlow(SearchQuery.Empty),
        tokenFilter: MutableStateFlow<(AccountStatus, CryptoCurrencyStatus) -> Boolean> =
            MutableStateFlow({ _, _ -> true }),
        portfolioTokenKeys: MutableStateFlow<Set<Pair<String, String>>> = MutableStateFlow(emptySet()),
    ): PredefinedTokensBlockDelegate = PredefinedTokensBlockDelegate(
        predefinedTokens = predefinedTokens,
        searchQueryState = searchQueryState,
        addToPortfolioManager = addToPortfolioManager,
        addToPortfolioSlot = addToPortfolioSlot,
        modelScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        tokenFilter = tokenFilter,
        portfolioTokenKeys = portfolioTokenKeys,
    )

    private fun currency(rawId: String, networkId: String): CryptoCurrencyStatus =
        mockk(relaxed = true) {
            every { currency.id.rawCurrencyId } returns CryptoCurrency.RawID(rawId)
            every { currency.network.rawId } returns networkId
        }

    private fun createPredefinedToken(
        id: String = "bitcoin",
        name: String = "Bitcoin",
        symbol: String = "BTC",
        networkId: String = ETHEREUM_NETWORK_ID,
        iconUrl: String? = null,
    ): PredefinedTokenToAdd = PredefinedTokenToAdd(
        token = RawMarketToken(id = CryptoCurrency.RawID(id), name = name, symbol = symbol),
        network = network(networkId),
        iconUrl = iconUrl,
    )

    private fun network(networkId: String): TokenMarketInfo.Network = TokenMarketInfo.Network(
        networkId = networkId,
        isExchangeable = false,
        contractAddress = null,
        decimalCount = 6,
    )

    internal data class FilterModel(val query: String, val expectedIds: List<String>?)

    @Suppress("UnusedPrivateMember")
    private fun provideTestModels() = listOf(
        FilterModel(
            query = "",
            expectedIds = listOf("bitcoin_$ETHEREUM_NETWORK_ID", "ethereum_$ETHEREUM_NETWORK_ID", "solana_$ETHEREUM_NETWORK_ID"),
        ),
        FilterModel(query = "btc", expectedIds = listOf("bitcoin_$ETHEREUM_NETWORK_ID")),
        FilterModel(query = "SOL", expectedIds = listOf("solana_$ETHEREUM_NETWORK_ID")),
        FilterModel(query = "ethereum", expectedIds = listOf("ethereum_$ETHEREUM_NETWORK_ID")),
        FilterModel(query = "zzz", expectedIds = null),
    )

    // endregion

    private companion object {
        const val ETHEREUM_NETWORK_ID = "ethereum"
    }
}