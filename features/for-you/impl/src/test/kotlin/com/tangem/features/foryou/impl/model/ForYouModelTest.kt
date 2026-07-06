package com.tangem.features.foryou.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletIcon
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
internal class ForYouModelTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val walletIconUMConverter: WalletIconUMConverter = mockk {
        every { convert(any()) } returns DeviceIconUM.Stub(cardsCount = 1)
    }
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk()

    private var model: ForYouModel? = null

    @BeforeEach
    fun setup() {
        every { getWalletIconUseCase.invoke(any()) } returns UserWalletIcon.Stub(cardsCount = 1)
        // Default: a real, non-empty emission so the model's `getOrElse { Default }` mapping path is
        // actually exercised in every test, not bypassed by an empty flow. Individual tests may override.
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Nested
    inner class InitialState {

        @Test
        fun `GIVEN model created WHEN not yet advanced THEN uiState is Loading`() = runTest {
            // Arrange
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(linkedMapOf())
            every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())

            // Act
            val model = createModel(testScope = this)

            // Assert — before advancing, the model exposes skeleton placeholder rows
            val loading = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Loading

            assertThat(loading.tokenList).hasSize(4)
            assertThat(loading.tokenList.all { it.tokenRowUM is TangemTokenRowUM.Loading }).isTrue()
        }
    }

    @Nested
    inner class ContentState {

        @Test
        fun `GIVEN wallets and account statuses emitted WHEN advanced THEN uiState becomes Content with tabs`() =
            runTest {
                // Arrange
                val walletOne = MockUserWalletFactory.create().copy(walletId = UserWalletId("01"), name = "Wallet 1")
                val walletTwo = MockUserWalletFactory.create().copy(walletId = UserWalletId("02"), name = "Wallet 2")
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(walletOne, walletTwo))
                every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(walletOne)

                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                val accountStatusList = createAccountStatusList(
                    userWalletId = walletOne.walletId,
                    currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                    totalFiatBalance = BigDecimal("100"),
                )
                every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
                    linkedMapOf(walletOne.walletId to accountStatusList),
                )
                every { getSelectedAppCurrencyUseCase() } returns flowOf()

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
                assertThat(content.assetCount).isNotNull()
                assertThat(model.uiState.value.walletListUM.items).hasSize(2)
                assertThat(model.uiState.value.walletListUM.items.map { it.isSelected }).containsExactly(true, false)
            }

        @Test
        fun `GIVEN exactly one wallet WHEN advanced THEN walletListUM items is empty`() = runTest {
            // Arrange
            val wallet = MockUserWalletFactory.create().copy(walletId = UserWalletId("01"), name = "Wallet 1")
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)

            val accountStatusList = createAccountStatusList(
                userWalletId = wallet.walletId,
                currencies = emptyList(),
                totalFiatBalance = BigDecimal.ZERO,
            )
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
                linkedMapOf(wallet.walletId to accountStatusList),
            )
            every { getSelectedAppCurrencyUseCase() } returns flowOf()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert — the "tabs.size != 1" rule: a single wallet shows no tabs
            assertThat(model.uiState.value.walletListUM.items).isEmpty()
        }
    }

    @Nested
    inner class TabClick {

        @Test
        fun `GIVEN two wallets WHEN onTabClick THEN locally selected wallet switches and currencies rederive`() =
            runTest {
                // Arrange
                val walletOne = MockUserWalletFactory.create().copy(walletId = UserWalletId("01"), name = "Wallet 1")
                val walletTwo = MockUserWalletFactory.create().copy(walletId = UserWalletId("02"), name = "Wallet 2")
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(walletOne, walletTwo))
                every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(walletOne)

                val btc = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                val eth = createCoin(rawCurrencyId = "eth", symbol = "ETH")
                val statusOne = createAccountStatusList(
                    userWalletId = walletOne.walletId,
                    currencies = listOf(createStatus(btc, loadedValue(BigDecimal("100")))),
                    totalFiatBalance = BigDecimal("100"),
                )
                val statusTwo = createAccountStatusList(
                    userWalletId = walletTwo.walletId,
                    currencies = listOf(createStatus(eth, loadedValue(BigDecimal("50")))),
                    totalFiatBalance = BigDecimal("50"),
                )
                every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
                    linkedMapOf(walletOne.walletId to statusOne, walletTwo.walletId to statusTwo),
                )
                every { getSelectedAppCurrencyUseCase() } returns flowOf()

                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act — click the second wallet's tab
                model.uiState.value.walletListUM.items[1].onClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.walletListUM.items.map { it.isSelected }).containsExactly(
                    false,
                    true,
                ).inOrder()
                val content = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
                val assetRow = content.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
                val titleUM = assetRow.titleUM as TangemTokenRowUM.TitleUM.Content
                assertThat(titleUM.text).isEqualTo(com.tangem.core.ui.extensions.stringReference("ETH"))
            }
    }

    @Nested
    inner class ExpandClick {

        @Test
        fun `GIVEN asset row clicked WHEN clicked again THEN isExpanded toggles back to false`() = runTest {
            // Arrange
            val wallet = MockUserWalletFactory.create().copy(walletId = UserWalletId("01"), name = "Wallet 1")
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
            every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)

            val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
            val accountStatusList = createAccountStatusList(
                userWalletId = wallet.walletId,
                currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                totalFiatBalance = BigDecimal("100"),
            )
            every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
                linkedMapOf(wallet.walletId to accountStatusList),
            )
            every { getSelectedAppCurrencyUseCase() } returns flowOf()

            val model = createModel(testScope = this)
            advanceUntilIdle()
            val initialContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(initialContent.tokenList.single().isExpanded).isFalse()

            // Act — click once to expand
            val assetRow = initialContent.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            assetRow.onItemClick?.invoke()
            advanceUntilIdle()

            // Assert
            val expandedContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(expandedContent.tokenList.single().isExpanded).isTrue()

            // Act — click again to collapse
            val expandedRow = expandedContent.tokenList.single().tokenRowUM as TangemTokenRowUM.Content
            expandedRow.onItemClick?.invoke()
            advanceUntilIdle()

            // Assert
            val collapsedContent = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
            assertThat(collapsedContent.tokenList.single().isExpanded).isFalse()
        }
    }

    @Nested
    inner class PeriodClick {

        @Test
        fun `GIVEN Content state WHEN period clicked THEN initialSelectedItem updates without resetting rest`() =
            runTest {
                // Arrange
                val wallet = MockUserWalletFactory.create().copy(walletId = UserWalletId("01"), name = "Wallet 1")
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
                every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)

                val currency = createCoin(rawCurrencyId = "btc", symbol = "BTC")
                val accountStatusList = createAccountStatusList(
                    userWalletId = wallet.walletId,
                    currencies = listOf(createStatus(currency, loadedValue(BigDecimal("100")))),
                    totalFiatBalance = BigDecimal("100"),
                )
                every { multiAccountStatusListSupplier.invokeAsMap() } returns flowOf(
                    linkedMapOf(wallet.walletId to accountStatusList),
                )
                every { getSelectedAppCurrencyUseCase() } returns flowOf()

                val model = createModel(testScope = this)
                advanceUntilIdle()
                val contentBefore = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
                val weekItem = contentBefore.periodPickerUM.items[1]
                val assetCountBefore = contentBefore.assetCount

                // Act
                contentBefore.onPeriodClick(weekItem)

                // Assert
                val contentAfter = model.uiState.value.portfolioReviewUM as PortfolioReviewUM.Content
                assertThat(contentAfter.periodPickerUM.initialSelectedItem).isEqualTo(weekItem)
                assertThat(contentAfter.assetCount).isEqualTo(assetCountBefore)
                assertThat(contentAfter.tokenList).isEqualTo(contentBefore.tokenList)
            }
    }

    private fun createModel(testScope: TestScope): ForYouModel {
        return ForYouModel(
            userWalletsListRepository = userWalletsListRepository,
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            walletIconUMConverter = walletIconUMConverter,
            getWalletIconUseCase = getWalletIconUseCase,
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun createAccountStatusList(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrencyStatus>,
        totalFiatBalance: BigDecimal,
    ): AccountStatusList = mockk {
        every { this@mockk.userWalletId } returns userWalletId
        every { flattenCurrencies() } returns currencies
        every { this@mockk.totalFiatBalance } returns TotalFiatBalance.Loaded(
            amount = totalFiatBalance,
            source = com.tangem.domain.models.StatusSource.ACTUAL,
        )
    }

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(fiatAmount: BigDecimal): CryptoCurrencyStatus.Loaded = mockk {
        every { amount } returns BigDecimal.ONE
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
    }

    private fun createCoin(rawCurrencyId: String, symbol: String): CryptoCurrency.Coin {
        val network: Network = mockk {
            every { name } returns "Network"
            every { isTestnet } returns false
            every { id } returns mockk { every { rawId } returns Network.RawID(rawCurrencyId) }
        }
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns "coin-$rawCurrencyId"
            every { this@mockk.rawCurrencyId } returns CryptoCurrency.RawID(rawCurrencyId)
        }
        return mockk<CryptoCurrency.Coin> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }
}