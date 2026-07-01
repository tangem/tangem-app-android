package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.card.WalletData
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.impl.choosetoken.market.MarketsListBatchFlowManager
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketBlockDelegateTest {

    private val marketsListBatchFlowManagerFactory: MarketsListBatchFlowManager.Factory = mockk()
    private val excludedBlockchains: ExcludedBlockchains = mockk(relaxed = true)
    private val getUserWalletsUseCase: GetWalletsUseCase = mockk(relaxed = true)
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()

    private val defaultManager: MarketsListBatchFlowManager = mockk(relaxed = true)
    private val searchManager: MarketsListBatchFlowManager = mockk(relaxed = true)

    private val searchQueryState = MutableStateFlow(SearchQuery.Empty)
    private val defaultUiItems = MutableStateFlow<ImmutableList<MarketsListItemUM>>(persistentListOf())

    // Keyed by the raw id value: CryptoCurrency.RawID is a value class, unboxed to String at the JVM boundary.
    private val tokenMarketsByRawId = mutableMapOf<String, TokenMarket>()

    @BeforeEach
    fun setup() {
        clearMocks(
            marketsListBatchFlowManagerFactory,
            addToPortfolioManagerFactory,
            singleAccountStatusListSupplier,
            defaultManager,
            searchManager,
        )
        searchQueryState.value = SearchQuery.Empty
        defaultUiItems.value = persistentListOf()
        tokenMarketsByRawId.clear()

        every {
            marketsListBatchFlowManagerFactory.create(
                GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
                any(),
                any(),
                any()
            )
        } returns defaultManager
        every {
            marketsListBatchFlowManagerFactory.create(
                GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
                any(),
                any(),
                any()
            )
        } returns searchManager
        every { addToPortfolioManagerFactory.create(any(), any(), any()) } returns mockk(relaxed = true)

        every { defaultManager.uiItems } returns defaultUiItems
        every { defaultManager.isInInitialLoadingErrorState } returns MutableStateFlow(false)
        every { defaultManager.totalCount } returns MutableStateFlow(null)
        every { defaultManager.getTokenMarketById(any()) } answers { tokenMarketsByRawId[firstArg<String>()] }

        every { searchManager.uiItems } returns MutableStateFlow(persistentListOf())
        every { searchManager.isInInitialLoadingErrorState } returns MutableStateFlow(false)
        every { searchManager.isSearchNotFoundState } returns MutableStateFlow(false)
        every { searchManager.totalCount } returns MutableStateFlow(null)
        every { searchManager.getTokenMarketById(any()) } returns null
    }

    @Test
    fun `GIVEN multi-currency wallet WHEN trending emitted THEN all items shown unchanged`() = runTest {
        // Arrange
        val item1 = marketItem("token-1")
        val item2 = marketItem("token-2")
        defaultUiItems.value = persistentListOf(item1, item2)
        val delegate = createDelegate(wallet = MockUserWalletFactory.create())

        // Act
        val result = lastMarketState(delegate)

        // Assert
        assertThat(result).isInstanceOf(SwapMarketState.Content::class.java)
        assertThat((result as SwapMarketState.Content).items).containsExactly(item1, item2).inOrder()
    }

    @Test
    fun `GIVEN single-currency wallet WHEN trending emitted THEN market block is hidden`() = runTest {
        // Arrange
        defaultUiItems.value = persistentListOf(marketItem("token-1"))
        val delegate = createDelegate(wallet = createSingleCurrencyWallet())

        // Act
        val result = lastMarketState(delegate)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN single-currency wallets not shown WHEN trending emitted THEN base state returned without filtering`() =
        runTest {
            // Arrange
            val item1 = marketItem("token-1")
            defaultUiItems.value = persistentListOf(item1)
            // Single-currency wallet would normally hide the block, but the setting short-circuits the per-wallet logic.
            val delegate = createDelegate(wallet = createSingleCurrencyWallet(), showSingleCurrencyWallets = false)

            // Act
            val result = lastMarketState(delegate)

            // Assert
            assertThat(result).isInstanceOf(SwapMarketState.Content::class.java)
            assertThat((result as SwapMarketState.Content).items).containsExactly(item1)
        }

    @Test
    fun `GIVEN NODL wallet WHEN trending emitted THEN only items on wallet network are shown`() = runTest {
        // Arrange
        val nodlWallet = MockUserWalletFactory.createSingleWalletWithToken()
        val itemOnWalletNetwork = marketItem("token-stellar")
        val itemOnOtherNetwork = marketItem("token-eth")
        tokenMarketsByRawId["token-stellar"] = tokenMarket(STELLAR_NETWORK_ID)
        tokenMarketsByRawId["token-eth"] = tokenMarket(ETHEREUM_NETWORK_ID)
        defaultUiItems.value = persistentListOf(itemOnWalletNetwork, itemOnOtherNetwork)

        every {
            singleAccountStatusListSupplier(nodlWallet.walletId)
        } returns flowOf(accountStatusList(STELLAR_NETWORK_ID))

        // Act
        val result = lastMarketState(createDelegate(wallet = nodlWallet))

        // Assert
        assertThat(result).isInstanceOf(SwapMarketState.Content::class.java)
        assertThat((result as SwapMarketState.Content).items).containsExactly(itemOnWalletNetwork)
        assertThat(result.total).isEqualTo(1)
    }

    @Test
    fun `GIVEN NODL wallet WHEN no trending tokens on wallet network THEN market block is hidden`() = runTest {
        // Arrange
        val nodlWallet = MockUserWalletFactory.createSingleWalletWithToken()
        val itemOnOtherNetwork = marketItem("token-eth")
        tokenMarketsByRawId["token-eth"] = tokenMarket(ETHEREUM_NETWORK_ID)
        defaultUiItems.value = persistentListOf(itemOnOtherNetwork)

        every {
            singleAccountStatusListSupplier(nodlWallet.walletId)
        } returns flowOf(accountStatusList(STELLAR_NETWORK_ID))

        // Act
        val result = lastMarketState(createDelegate(wallet = nodlWallet))

        // Assert
        assertThat(result).isNull()
    }

    // region Helpers

    private fun TestScope.lastMarketState(delegate: MarketBlockDelegate): SwapMarketState? {
        val emittedValues = getEmittedValues(delegate.marketsStateFlow)
        advanceUntilIdle()
        return emittedValues.last()
    }

    private fun TestScope.createDelegate(
        wallet: UserWallet,
        showSingleCurrencyWallets: Boolean = true,
    ): MarketBlockDelegate {
        val selectedWalletFlow = MutableSharedFlow<UserWallet>(replay = 1)
        selectedWalletFlow.tryEmit(wallet)
        return MarketBlockDelegate(
            marketsListBatchFlowManagerFactory = marketsListBatchFlowManagerFactory,
            excludedBlockchains = excludedBlockchains,
            getUserWalletsUseCase = getUserWalletsUseCase,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            modelScope = backgroundScope,
            searchQueryState = searchQueryState,
            screensSourcesName = "test",
            selectedWalletFlow = selectedWalletFlow,
            shouldShowSingleCurrencyWallets = showSingleCurrencyWallets,
        )
    }

    private fun marketItem(id: String): MarketsListItemUM = mockk {
        every { this@mockk.id } returns CryptoCurrency.RawID(id)
    }

    private fun tokenMarket(vararg networkIds: String): TokenMarket = mockk {
        every { networks } returns networkIds.map { networkId ->
            TokenMarket.Network(networkId = networkId, contractAddress = null, decimalCount = null)
        }
    }

    private fun accountStatusList(vararg networkIds: String): AccountStatusList = mockk {
        every { flattenCurrencies() } returns networkIds.map { networkId ->
            mockk<CryptoCurrencyStatus> {
                every { currency.network.rawId } returns networkId
            }
        }
    }

    private fun createSingleCurrencyWallet(): UserWallet.Cold = UserWallet.Cold(
        name = "Single",
        walletId = UserWalletId("022"),
        cardsInWallet = emptySet(),
        isMultiCurrency = false,
        scanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 2),
            derivedKeys = emptyMap(),
        ).copy(
            productType = ProductType.Note,
            walletData = WalletData(blockchain = "XLM", token = null),
        ),
        hasBackupError = false,
    )

    // endregion

    private companion object {
        const val STELLAR_NETWORK_ID = "stellar"
        const val ETHEREUM_NETWORK_ID = "ethereum"
    }
}