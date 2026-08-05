package com.tangem.features.foryou.impl.tokensummary.swapchooser.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletIcon
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.foryou.impl.tokensummary.model.SwapHolding
import com.tangem.features.foryou.impl.tokensummary.swapchooser.SwapTokenChooserComponent
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * The chooser only renders the holdings resolved by the parent, so the tests drive
 * [SwapTokenChooserComponent.Params.holdings] and assert what lands in [SwapTokenChooserModel.content].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTokenChooserModelTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = currencyFactory.createCoin(Blockchain.Ethereum)
    private val ethereumStatus = CryptoCurrencyStatus(currency = ethereum, value = loadedValue())

    private val holdings = MutableStateFlow<List<SwapHolding>>(value = emptyList())

    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()

    // Hot maps to a plain DeviceIconUM.Mobile, so wallet headers render without touching Android colour parsing.
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk {
        every { this@mockk.invoke(any()) } returns UserWalletIcon.Hot
    }
    private val callbacks: SwapTokenChooserComponent.ModelCallbacks = mockk(relaxUnitFun = true)

    @BeforeEach
    fun setup() {
        clearMocks(callbacks)

        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        every { isAccountsModeEnabledUseCase.invoke() } returns flowOf(false)

        holdings.value = listOf(holding(WALLET_ID), holding(OTHER_WALLET_ID))
    }

    @Test
    fun `GIVEN holdings in two wallets WHEN model created THEN each is rendered under its own wallet header`() =
        runTest {
            // Arrange
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            val sections = model.content.value?.sections.orEmpty()
            assertThat(sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()).hasSize(2)
            assertThat(sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()).hasSize(2)
            verify(exactly = 0) { callbacks.onDismiss() }

            model.onDestroy()
        }

    @Test
    fun `GIVEN a rendered holding WHEN it is clicked THEN the holding it came from is reported`() = runTest {
        // Arrange — an unavailable holding is rendered like any other; the parent decides what its click does
        val unavailable = holding(OTHER_WALLET_ID, ScenarioUnavailabilityReason.SingleWallet)
        holdings.value = listOf(unavailable)
        val model = createModel()
        advanceUntilIdle()

        // Act
        val item = model.content.value
            ?.sections
            ?.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
            ?.single()
            ?.items
            ?.single()
        item?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { callbacks.onHoldingSelected(unavailable) }
        verify(exactly = 0) { callbacks.onDismiss() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN a rendered holding WHEN it stops qualifying THEN the sheet dismisses itself`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        holdings.value = emptyList()
        advanceUntilIdle()

        // Assert — the parent only opens the chooser for holdings it has, so an empty list is not a loading state
        assertThat(model.content.value).isNull()
        verify(exactly = 1) { callbacks.onDismiss() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN balance hiding is on WHEN model created THEN balances are hidden`() = runTest {
        // Arrange
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(true)
        holdings.value = listOf(holding(WALLET_ID))
        val model = createModel()

        // Act
        advanceUntilIdle()

        // Assert
        val item = model.content.value
            ?.sections
            ?.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
            ?.single()
            ?.items
            ?.single()
        assertThat(item?.isBalanceHidden).isTrue()

        model.onDestroy()
    }

    // region arrange helpers

    private fun holding(
        walletId: UserWalletId,
        reason: ScenarioUnavailabilityReason = ScenarioUnavailabilityReason.None,
    ) = SwapHolding(entry = entry(walletId), unavailabilityReason = reason)

    private fun entry(walletId: UserWalletId) = TokenSelectorEntry(
        wallet = wallet(walletId),
        account = portfolio(walletId),
        currencyStatus = ethereumStatus,
    )

    private fun wallet(walletId: UserWalletId): UserWallet =
        MockUserWalletFactory.create().copy(walletId = walletId, isMultiCurrency = true)

    private fun portfolio(walletId: UserWalletId): AccountStatus.CryptoPortfolio = mockk {
        // `account` is dereferenced for grouping and item ids, so it is a real model rather than a stub.
        every { account } returns MockAccounts.createAccount(derivationIndex = 1, userWalletId = walletId)
        every { tokenList } returns mockk<TokenList>()
    }

    private fun TestScope.createModel(): SwapTokenChooserModel {
        val params = SwapTokenChooserComponent.Params(holdings = holdings, callbacks = callbacks)

        return SwapTokenChooserModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            getWalletIconUseCase = getWalletIconUseCase,
            walletIconUMConverter = WalletIconUMConverter(),
        )
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

    // endregion

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val OTHER_WALLET_ID = UserWalletId("02")

        fun loadedValue() = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = mockk(relaxed = true),
            sources = CryptoCurrencyStatus.Sources(),
        )
    }
}