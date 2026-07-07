package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.getTotalWithRewardsStakingBalance
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SetBalanceTransformerTest {

    private val onToggleBalanceType: () -> Unit = mockk(relaxed = true)
    private val appCurrency: AppCurrency = AppCurrency.Default

    private val network: Network = mockk(relaxed = true) {
        every { rawId } returns "ethereum"
    }
    private val currency: CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.network } returns this@SetBalanceTransformerTest.network
        every { symbol } returns "ETH"
    }

    @BeforeEach
    fun setup() {
        mockkStatic(StakingBalance.Data::getTotalWithRewardsStakingBalance)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(StakingBalance.Data::getTotalWithRewardsStakingBalance)
    }

    // region Status type → BalanceBlock type mapping

    @Test
    fun `GIVEN Loading status WHEN transform THEN balance block is Loading`() {
        // GIVEN
        val status = createStatus(CryptoCurrencyStatus.Loading)
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Loading::class.java)
    }

    @Test
    fun `GIVEN Loaded status WHEN transform THEN balance block is Content`() {
        // GIVEN
        val status = createStatus(loadedValue())
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Content::class.java)
    }

    @Test
    fun `GIVEN NoQuote status WHEN transform THEN balance block is Content`() {
        // GIVEN
        val status = createStatus(noQuoteValue())
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Content::class.java)
    }

    @Test
    fun `GIVEN NoAccount status WHEN transform THEN balance block is Content`() {
        // GIVEN
        val status = createStatus(noAccountValue())
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Content::class.java)
    }

    @Test
    fun `GIVEN Custom status WHEN transform THEN balance block is Content`() {
        // GIVEN
        val status = createStatus(customValue())
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Content::class.java)
    }

    @Test
    fun `GIVEN Unreachable status WHEN transform THEN balance block is Error`() {
        // GIVEN
        val status = createStatus(
            CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
        )
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Error::class.java)
    }

    @Test
    fun `GIVEN NoAmount status WHEN transform THEN balance block is Error`() {
        // GIVEN
        val status = createStatus(CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Error::class.java)
    }

    @Test
    fun `GIVEN MissedDerivation status WHEN transform THEN balance block is Error`() {
        // GIVEN
        val status = createStatus(CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM).isInstanceOf(TokenDetailsBalanceBlockUM.Error::class.java)
    }

    // endregion

    // region Action buttons & icon preservation

    @Test
    fun `GIVEN any loaded status WHEN transform THEN action buttons are preserved`() {
        // GIVEN
        val status = createStatus(loadedValue())
        val transformer = createTransformer(status)
        val state = initialState()

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.balanceBlockUM.addFundsButton).isEqualTo(state.balanceBlockUM.addFundsButton)
        assertThat(result.balanceBlockUM.swapButton).isEqualTo(state.balanceBlockUM.swapButton)
        assertThat(result.balanceBlockUM.transferButton).isEqualTo(state.balanceBlockUM.transferButton)
    }

    @Test
    fun `GIVEN any loaded status WHEN transform THEN currency icon state is preserved`() {
        // GIVEN
        val status = createStatus(loadedValue())
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.balanceBlockUM.currencyIconState)
            .isEqualTo(initialState().balanceBlockUM.currencyIconState)
    }

    // endregion

    // region Staking / balance type

    @Test
    fun `GIVEN loaded status without staking WHEN transform THEN balance type is Single`() {
        // GIVEN
        val status = createStatus(loadedValue(stakingBalance = null))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.tokenBalanceTypeUM).isEqualTo(TokenBalanceTypeUM.Single)
    }

    @Test
    fun `GIVEN loaded status with staking WHEN transform THEN balance type is Multiple`() {
        // GIVEN
        val stakingBalance: StakingBalance.Data = mockk(relaxed = true)
        every { stakingBalance.getTotalWithRewardsStakingBalance(any()) } returns BigDecimal("1.5")
        val status = createStatus(loadedValue(stakingBalance = stakingBalance))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.tokenBalanceTypeUM).isInstanceOf(TokenBalanceTypeUM.Multiple::class.java)
    }

    @Test
    fun `GIVEN loaded status with staking WHEN transform THEN available balance types include ALL and AVAILABLE`() {
        // GIVEN
        val stakingBalance: StakingBalance.Data = mockk(relaxed = true)
        every { stakingBalance.getTotalWithRewardsStakingBalance(any()) } returns BigDecimal("1.5")
        val status = createStatus(loadedValue(stakingBalance = stakingBalance))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val multiple = (result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content)
            .tokenBalanceTypeUM as TokenBalanceTypeUM.Multiple
        assertThat(multiple.availableTypes).containsExactly(
            TokenBalanceTypeUM.Type.ALL,
            TokenBalanceTypeUM.Type.AVAILABLE,
        )
    }

    @Test
    fun `GIVEN staking balance WHEN Multiple onSelect invoked THEN onToggleBalanceType is called`() {
        // GIVEN
        val stakingBalance: StakingBalance.Data = mockk(relaxed = true)
        every { stakingBalance.getTotalWithRewardsStakingBalance(any()) } returns BigDecimal("1.5")
        val status = createStatus(loadedValue(stakingBalance = stakingBalance))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())
        val multiple = (result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content)
            .tokenBalanceTypeUM as TokenBalanceTypeUM.Multiple
        multiple.onSelect()

        // THEN
        verify(exactly = 1) { onToggleBalanceType.invoke() }
    }

    @Test
    fun `GIVEN staking and previous Multiple type AVAILABLE WHEN transform THEN selected type is preserved`() {
        // GIVEN
        val stakingBalance: StakingBalance.Data = mockk(relaxed = true)
        every { stakingBalance.getTotalWithRewardsStakingBalance(any()) } returns BigDecimal("1.5")
        val status = createStatus(loadedValue(stakingBalance = stakingBalance))
        val transformer = createTransformer(status)

        val prevContent = TokenDetailsBalanceBlockUM.Content(
            addFundsButton = placeholderButton(),
            swapButton = placeholderButton(),
            transferButton = placeholderButton(),
            tokenBalanceTypeUM = TokenBalanceTypeUM.Multiple(
                type = TokenBalanceTypeUM.Type.AVAILABLE,
                availableTypes = persistentListOf(TokenBalanceTypeUM.Type.ALL, TokenBalanceTypeUM.Type.AVAILABLE),
                onSelect = {},
            ),
            currencyIconState = CurrencyIconState.Loading,
            displayCryptoBalanceAll = stringReference(""),
            displayFiatBalanceAll = stringReference(""),
            displayCryptoBalanceAvailable = null,
            displayFiatBalanceAvailable = null,
            isBalanceFlickering = false,
            isBalanceZero = false,
        )
        val state = initialState().copy(balanceBlockUM = prevContent)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val multiple = (result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content)
            .tokenBalanceTypeUM as TokenBalanceTypeUM.Multiple
        assertThat(multiple.type).isEqualTo(TokenBalanceTypeUM.Type.AVAILABLE)
    }

    // endregion

    // region Balance flickering

    @Test
    fun `GIVEN CACHE source WHEN transform THEN isBalanceFlickering is true`() {
        // GIVEN
        val sources = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.CACHE,
            quoteSource = StatusSource.CACHE,
            stakingBalanceSource = StatusSource.CACHE,
        )
        val status = createStatus(loadedValue(sources = sources))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.isBalanceFlickering).isTrue()
    }

    @Test
    fun `GIVEN ACTUAL source WHEN transform THEN isBalanceFlickering is false`() {
        // GIVEN
        val sources = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.ACTUAL,
            quoteSource = StatusSource.ACTUAL,
            stakingBalanceSource = StatusSource.ACTUAL,
        )
        val status = createStatus(loadedValue(sources = sources))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.isBalanceFlickering).isFalse()
    }

    // endregion

    // region isBalanceZero

    @Test
    fun `GIVEN amount is zero WHEN transform THEN isBalanceZero is true`() {
        // GIVEN
        val status = createStatus(loadedValue(amount = BigDecimal.ZERO, stakingBalance = null))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.isBalanceZero).isTrue()
    }

    @Test
    fun `GIVEN non-zero amount WHEN transform THEN isBalanceZero is false`() {
        // GIVEN
        val status = createStatus(loadedValue(amount = BigDecimal("0.001"), stakingBalance = null))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.isBalanceZero).isFalse()
    }

    @Test
    fun `GIVEN zero amount but non-zero staking WHEN transform THEN isBalanceZero is false`() {
        // GIVEN — staking balance counts towards "total" so amount+staking != 0 keeps the rich UI
        val stakingBalance: StakingBalance.Data = mockk(relaxed = true)
        every { stakingBalance.getTotalWithRewardsStakingBalance(any()) } returns BigDecimal("1.5")
        val status = createStatus(loadedValue(amount = BigDecimal.ZERO, stakingBalance = stakingBalance))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.isBalanceZero).isFalse()
    }

    // endregion

    // region No staking → available balances

    @Test
    fun `GIVEN loaded without staking WHEN transform THEN available balances are null`() {
        // GIVEN
        val status = createStatus(loadedValue(stakingBalance = null))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayCryptoBalanceAvailable).isNull()
        assertThat(content.displayFiatBalanceAvailable).isNull()
    }

    @Test
    fun `GIVEN staking balance WHEN transform THEN available balances are not null`() {
        // GIVEN
        val stakingBalance: StakingBalance.Data = mockk(relaxed = true)
        every { stakingBalance.getTotalWithRewardsStakingBalance(any()) } returns BigDecimal("1.5")
        val status = createStatus(loadedValue(stakingBalance = stakingBalance))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayCryptoBalanceAvailable).isNotNull()
        assertThat(content.displayFiatBalanceAvailable).isNotNull()
    }

    // endregion

    // region Yield supply balance

    @Test
    fun `GIVEN yield active AND prev has yield balances WHEN transform THEN yield balances preserved`() {
        // GIVEN
        val status = createStatus(loadedValue(yieldSupplyStatus = activeYieldSupplyStatus()))
        val transformer = createTransformer(status)
        val prev = contentWithYieldBalances(fiat = "$21,000.12", crypto = "10.500001 ETH")
        val state = initialState().copy(balanceBlockUM = prev)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayYieldSupplyFiatBalance).isEqualTo("$21,000.12")
        assertThat(content.displayYieldSupplyCryptoBalance).isEqualTo("10.500001 ETH")
    }

    @Test
    fun `GIVEN yield inactive AND prev has yield balances WHEN transform THEN yield balances are cleared`() {
        // GIVEN
        val status = createStatus(loadedValue(yieldSupplyStatus = null))
        val transformer = createTransformer(status)
        val prev = contentWithYieldBalances(fiat = "$21,000.12", crypto = "10.500001 ETH")
        val state = initialState().copy(balanceBlockUM = prev)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayYieldSupplyFiatBalance).isNull()
        assertThat(content.displayYieldSupplyCryptoBalance).isNull()
    }

    @Test
    fun `GIVEN yield active AND prev is not Content WHEN transform THEN yield balances are null`() {
        // GIVEN — prev is the default Loading block, so there are no yield balances to preserve yet
        val status = createStatus(loadedValue(yieldSupplyStatus = activeYieldSupplyStatus()))
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val content = result.balanceBlockUM as TokenDetailsBalanceBlockUM.Content
        assertThat(content.displayYieldSupplyFiatBalance).isNull()
        assertThat(content.displayYieldSupplyCryptoBalance).isNull()
    }

    // endregion

    // region Unrelated fields preserved

    @Test
    fun `GIVEN any status WHEN transform THEN unrelated fields are preserved`() {
        // GIVEN
        val state = initialState()
        val status = createStatus(loadedValue())
        val transformer = createTransformer(status)

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
        assertThat(result.marketPriceBlockState).isSameInstanceAs(state.marketPriceBlockState)
        assertThat(result.earnBlockState).isEqualTo(state.earnBlockState)
        assertThat(result.pullToRefreshConfig).isSameInstanceAs(state.pullToRefreshConfig)
        assertThat(result.isBalanceHidden).isEqualTo(state.isBalanceHidden)
        assertThat(result.isMarketPriceAvailable).isEqualTo(state.isMarketPriceAvailable)
    }

    // endregion

    private fun createTransformer(status: CryptoCurrencyStatus) = SetBalanceTransformer(
        status = status,
        appCurrency = appCurrency,
        onToggleBalanceType = onToggleBalanceType,
    )

    private fun createStatus(value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(
        amount: BigDecimal = BigDecimal("10.5"),
        fiatAmount: BigDecimal = BigDecimal("21000"),
        fiatRate: BigDecimal = BigDecimal("2000"),
        stakingBalance: StakingBalance? = null,
        yieldSupplyStatus: YieldSupplyStatus? = null,
        sources: CryptoCurrencyStatus.Sources = CryptoCurrencyStatus.Sources(),
    ): CryptoCurrencyStatus.Loaded = CryptoCurrencyStatus.Loaded(
        amount = amount,
        fiatAmount = fiatAmount,
        fiatRate = fiatRate,
        priceChange = BigDecimal("2.5"),
        stakingBalance = stakingBalance,
        yieldSupplyStatus = yieldSupplyStatus,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = mockk(relaxed = true),
        sources = sources,
    )

    private fun activeYieldSupplyStatus(): YieldSupplyStatus = YieldSupplyStatus(
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = null,
    )

    private fun contentWithYieldBalances(
        fiat: String?,
        crypto: String?,
    ): TokenDetailsBalanceBlockUM.Content = TokenDetailsBalanceBlockUM.Content(
        addFundsButton = placeholderButton(),
        swapButton = placeholderButton(),
        transferButton = placeholderButton(),
        tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
        currencyIconState = CurrencyIconState.Loading,
        displayCryptoBalanceAll = stringReference(""),
        displayFiatBalanceAll = stringReference(""),
        displayCryptoBalanceAvailable = null,
        displayFiatBalanceAvailable = null,
        isBalanceFlickering = false,
        isBalanceZero = false,
        displayYieldSupplyFiatBalance = fiat,
        displayYieldSupplyCryptoBalance = crypto,
    )

    private fun noQuoteValue(): CryptoCurrencyStatus.NoQuote = CryptoCurrencyStatus.NoQuote(
        amount = BigDecimal("5.0"),
        stakingBalance = null,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = mockk(relaxed = true),
        sources = CryptoCurrencyStatus.Sources(),
    )

    private fun noAccountValue(): CryptoCurrencyStatus.NoAccount = CryptoCurrencyStatus.NoAccount(
        amountToCreateAccount = BigDecimal("0.01"),
        fiatAmount = BigDecimal.ZERO,
        priceChange = null,
        fiatRate = BigDecimal("2000"),
        networkAddress = mockk(relaxed = true),
        sources = CryptoCurrencyStatus.Sources(),
    )

    private fun customValue(): CryptoCurrencyStatus.Custom = CryptoCurrencyStatus.Custom(
        amount = BigDecimal("100"),
        fiatAmount = null,
        fiatRate = null,
        priceChange = null,
        stakingBalance = null,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = mockk(relaxed = true),
        sources = CryptoCurrencyStatus.Sources(),
    )

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
            addFundsButton = placeholderButton(),
            swapButton = placeholderButton(),
            transferButton = placeholderButton(),
            tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
            currencyIconState = CurrencyIconState.Loading,
        ),
        notifications = persistentListOf(),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        earnBlockState = null,
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )

    private fun placeholderButton(): TangemButtonUM = TangemButtonUM(
        text = stringReference(""),
        type = TangemButtonType.Secondary,
        onClick = {},
    )
}