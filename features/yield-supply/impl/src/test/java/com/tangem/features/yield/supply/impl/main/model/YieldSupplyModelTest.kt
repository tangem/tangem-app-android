package com.tangem.features.yield.supply.impl.main.model

import arrow.core.Option
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.promo.usecase.GetBoostedApyUseCase
import com.tangem.domain.yield.supply.promo.usecase.IsYieldBoostPromoEnabledForTokenUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyActivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEnterStatusFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEnterStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetDustMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyIsAvailableUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyModelTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private val getTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk()
    private val isAvailableUseCase: YieldSupplyIsAvailableUseCase = mockk()
    private val activateUseCase: YieldSupplyActivateUseCase = mockk()
    private val deactivateUseCase: YieldSupplyDeactivateUseCase = mockk()
    private val enterStatusUseCase: YieldSupplyEnterStatusUseCase = mockk()
    private val enterStatusFlowUseCase: YieldSupplyEnterStatusFlowUseCase = mockk()
    private val minAmountUseCase: YieldSupplyMinAmountUseCase = mockk()
    private val getDustMinAmountUseCase: YieldSupplyGetDustMinAmountUseCase = mockk()
    private val isBoostPromoEnabledUseCase: IsYieldBoostPromoEnabledForTokenUseCase = mockk()
    private val getBoostedApyUseCase = GetBoostedApyUseCase()
    private val boostStoryPreloader: YieldBoostStoryPreloader = mockk(relaxed = true)

    private val userWalletId = UserWalletId("abcdef012345")
    private val userWallet: UserWallet = mockk(relaxed = true) { every { walletId } returns userWalletId }
    private val token: CryptoCurrency.Token = token()
    private val coin: CryptoCurrency.Coin = coin()
    private val accountStatusList: AccountStatusList = mockk()

    @BeforeEach
    fun setUp() {
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { isAvailableUseCase(any(), any()) } returns true
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountStatusList)
        every { enterStatusFlowUseCase(any(), any()) } returns flowOf(null)
        coEvery { enterStatusUseCase(any(), any()) } returns null.right()
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
        coEvery { getTokenStatusUseCase(any()) } returns marketToken(isActive = true).right()
        coEvery { isBoostPromoEnabledUseCase(any(), any()) } returns false.right()
        coEvery { activateUseCase(any(), any(), any()) } returns true.right()
        coEvery { deactivateUseCase(any(), any()) } returns true.right()
        coEvery { minAmountUseCase(any(), any()) } returns BigDecimal("5").right()
        every { getDustMinAmountUseCase(any(), any(), any()) } returns BigDecimal("0.1")
        stubStatus(status(isActive = false).some())
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN yield supply unavailable WHEN model created THEN stays initial and skips wallet load`() = runTest {
        // Arrange
        coEvery { isAvailableUseCase(any(), any()) } returns false

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Initial)
        assertThat(model.uiState.value).isNull()
        verify(exactly = 0) { getUserWalletUseCase(any()) }
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN wallet load fails WHEN model created THEN stays initial and skips status subscription`() = runTest {
        // Arrange
        every { getUserWalletUseCase(userWalletId) } returns mockk<GetUserWalletError>(relaxed = true).left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Initial)
        verify(exactly = 0) { accountStatusListSupplier(any<UserWalletId>()) }
        coVerify(exactly = 1) { singleNetworkStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN inactive token with active market WHEN status emitted THEN available state without boost`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val legacy = model.uiStateLegacy.value
        assertThat(legacy).isInstanceOf(YieldSupplyUM.Available::class.java)
        assertThat((legacy as YieldSupplyUM.Available).isBoostAvailable).isFalse()
        assertThat(legacy.apy).isEqualTo("5")

        val block = model.uiState.value
        assertThat(block).isInstanceOf(EarnBlockUM.Content::class.java)
        assertThat((block as EarnBlockUM.Content).backgroundUM).isEqualTo(EarnBlockUM.BackgroundUM.AccentSoft)
    }

    @Test
    fun `GIVEN promo enabled for token WHEN status emitted THEN boosted available promo`() = runTest {
        // Arrange
        coEvery { isBoostPromoEnabledUseCase(any(), any()) } returns true.right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val legacy = model.uiStateLegacy.value
        assertThat(legacy).isInstanceOf(YieldSupplyUM.Available::class.java)
        assertThat((legacy as YieldSupplyUM.Available).isBoostAvailable).isTrue()
        assertThat(model.uiState.value).isInstanceOf(EarnBlockUM.Promo::class.java)
    }

    @Test
    fun `GIVEN app currency unavailable WHEN status emitted THEN falls back to default and still loads`() = runTest {
        // Arrange
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns SelectedAppCurrencyError.NoAppCurrencySelected.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isInstanceOf(YieldSupplyUM.Available::class.java)
    }

    @Test
    fun `GIVEN inactive token with inactive market WHEN status emitted THEN unavailable and no block`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns marketToken(isActive = false).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Unavailable)
        assertThat(model.uiState.value).isNull()
    }

    @Test
    fun `GIVEN inactive token and token status fails WHEN status emitted THEN resets to initial`() = runTest {
        // Arrange
        coEvery { getTokenStatusUseCase(any()) } returns Throwable("boom").left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Initial)
    }

    @Test
    fun `GIVEN active token allowed to spend WHEN status emitted THEN content without warning icon`() = runTest {
        // Arrange — supplied fully so the info-icon branch stays off
        stubStatus(status(isActive = true, effectiveProtocolBalance = BigDecimal.TEN).some())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val legacy = model.uiStateLegacy.value
        assertThat(legacy).isInstanceOf(YieldSupplyUM.Content::class.java)
        assertThat((legacy as YieldSupplyUM.Content).shouldShowWarningIcon).isFalse()
        assertThat(legacy.shouldShowInfoIcon).isFalse()
        verify(exactly = 0) { analytics.send(any<YieldSupplyAnalytics.NoticeApproveNeeded>()) }
    }

    @Test
    fun `GIVEN active token not allowed to spend WHEN status emitted THEN warning icon and analytics sent`() = runTest {
        // Arrange
        stubStatus(status(isActive = true, isAllowedToSpend = false, effectiveProtocolBalance = BigDecimal.TEN).some())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val legacy = model.uiStateLegacy.value as YieldSupplyUM.Content
        assertThat(legacy.shouldShowWarningIcon).isTrue()
        val events = mutableListOf<AnalyticsEvent>()
        verify { analytics.send(capture(events)) }
        val approveEvent = events.filterIsInstance<YieldSupplyAnalytics.NoticeApproveNeeded>().single()
        assertThat(approveEvent.token).isEqualTo("TTK")
        assertThat(approveEvent.blockchain).isEqualTo("Ethereum")

        val block = model.uiState.value as EarnBlockUM.Content
        assertThat(block.titleUM.iconUM?.tone).isEqualTo(EarnBlockUM.TitleUM.IconTone.Warning)
    }

    @Test
    fun `GIVEN active token and token status fails WHEN status emitted THEN content with empty apy`() = runTest {
        // Arrange
        stubStatus(status(isActive = true, effectiveProtocolBalance = BigDecimal.TEN).some())
        coEvery { getTokenStatusUseCase(any()) } returns Throwable("boom").left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val legacy = model.uiStateLegacy.value as YieldSupplyUM.Content
        assertThat(legacy.apy).isEmpty()
    }

    @Test
    fun `GIVEN active token with not supplied amount WHEN status emitted THEN info icon shown`() = runTest {
        // Arrange — amount(10) > protocolBalance(1) so there is a not-supplied remainder above the dust limit
        stubStatus(status(isActive = true, effectiveProtocolBalance = BigDecimal.ONE).some())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val legacy = model.uiStateLegacy.value as YieldSupplyUM.Content
        assertThat(legacy.shouldShowInfoIcon).isTrue()
        assertThat(legacy.shouldShowWarningIcon).isFalse()
        val block = model.uiState.value as EarnBlockUM.Content
        assertThat(block.titleUM.iconUM?.tone).isEqualTo(EarnBlockUM.TitleUM.IconTone.Info)
    }

    @Test
    fun `GIVEN not supplied amount below dust WHEN status emitted THEN info icon hidden`() = runTest {
        // Arrange — dust threshold far above the not-supplied fiat value
        stubStatus(status(isActive = true, effectiveProtocolBalance = BigDecimal.ONE).some())
        every { getDustMinAmountUseCase(any(), any(), any()) } returns BigDecimal("1000")

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat((model.uiStateLegacy.value as YieldSupplyUM.Content).shouldShowInfoIcon).isFalse()
    }

    @Test
    fun `GIVEN not supplied amount but min amount unavailable WHEN status emitted THEN info icon hidden`() = runTest {
        // Arrange — not-supplied remainder exists, but the min-amount lookup fails
        stubStatus(status(isActive = true, effectiveProtocolBalance = BigDecimal.ONE).some())
        coEvery { minAmountUseCase(any(), any()) } returns Throwable("no min").left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat((model.uiStateLegacy.value as YieldSupplyUM.Content).shouldShowInfoIcon).isFalse()
        verify(exactly = 0) { getDustMinAmountUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN pending enter status WHEN status emitted THEN processing enter`() = runTest {
        // Arrange
        coEvery { enterStatusUseCase(any(), any()) } returns YieldSupplyPendingStatus.Enter(txIds = listOf("0x1")).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Processing.Enter)
        assertThat(model.uiState.value).isInstanceOf(EarnBlockUM.Content::class.java)
    }

    @Test
    fun `GIVEN pending exit status WHEN status emitted THEN processing exit`() = runTest {
        // Arrange
        coEvery { enterStatusUseCase(any(), any()) } returns YieldSupplyPendingStatus.Exit(txIds = listOf("0x1")).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Processing.Exit)
    }

    @Test
    fun `GIVEN processing state WHEN cached status emitted THEN keeps processing`() = runTest {
        // Arrange — first emission sets Processing.Enter, second (from cache) must be ignored
        val firstList: AccountStatusList = mockk()
        val secondList: AccountStatusList = mockk()
        val supplierFlow = MutableStateFlow(firstList)
        every { accountStatusListSupplier(userWalletId) } returns supplierFlow
        stubStatus(status(isActive = false, amount = BigDecimal.TEN).some(), firstList)
        stubStatus(
            option = status(isActive = false, amount = BigDecimal.ONE, networkSource = StatusSource.CACHE).some(),
            list = secondList,
        )
        coEvery { enterStatusUseCase(any(), any()) } returns
            YieldSupplyPendingStatus.Enter(txIds = listOf("0x1")).right()

        // Act
        val model = createModel()
        advanceUntilIdle()
        supplierFlow.value = secondList
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Processing.Enter)
        coVerify(exactly = 1) { enterStatusUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN identical statuses emitted twice WHEN model created THEN downstream runs once`() = runTest {
        // Arrange — distinctUntilChanged must collapse equal emissions
        val firstList: AccountStatusList = mockk()
        val secondList: AccountStatusList = mockk()
        val sameStatus = status(isActive = false)
        every { accountStatusListSupplier(userWalletId) } returns flowOf(firstList, secondList)
        stubStatus(sameStatus.some(), firstList)
        stubStatus(sameStatus.some(), secondList)

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { enterStatusUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN two distinct emissions WHEN model created THEN protocol status sent only on the first`() = runTest {
        // Arrange — first emission active, second inactive; the once-only compareAndSet must fire sendInfo on the first
        // only. If the guard were removed, the second (inactive) emission would call deactivate.
        val firstList: AccountStatusList = mockk()
        val secondList: AccountStatusList = mockk()
        every { accountStatusListSupplier(userWalletId) } returns flowOf(firstList, secondList)
        stubStatus(
            status(isActive = true, amount = BigDecimal.TEN, effectiveProtocolBalance = BigDecimal.TEN).some(),
            firstList,
        )
        stubStatus(
            status(isActive = false, amount = BigDecimal.ONE).some(),
            secondList,
        )

        // Act
        createModel()
        advanceUntilIdle()

        // Assert — activate fired once (first emission); the guard suppressed the second, so deactivate never ran
        coVerify(exactly = 1) { activateUseCase(userWalletId, token, SOURCE_ADDRESS) }
        coVerify(exactly = 0) { deactivateUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN cached status while not processing WHEN status emitted THEN state still advances`() = runTest {
        // Arrange — the cache guard must short-circuit ONLY while Processing
        stubStatus(status(isActive = false, networkSource = StatusSource.CACHE).some())

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isInstanceOf(YieldSupplyUM.Available::class.java)
    }

    @Test
    fun `GIVEN coin currency WHEN status emitted THEN token-only logic is skipped`() = runTest {
        // Arrange — every token-specific step guards on CryptoCurrency.Token
        stubStatus(status(currency = coin, isActive = false).some())

        // Act
        val model = createModel(currency = coin)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiStateLegacy.value).isEqualTo(YieldSupplyUM.Initial)
        coVerify(exactly = 0) { getTokenStatusUseCase(any()) }
        coVerify(exactly = 0) { activateUseCase(any(), any(), any()) }
        coVerify(exactly = 0) { deactivateUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN active status on first emission WHEN model created THEN activates protocol`() = runTest {
        // Arrange
        stubStatus(status(isActive = true, effectiveProtocolBalance = BigDecimal.TEN).some())

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        coVerify { activateUseCase(userWalletId, token, SOURCE_ADDRESS) }
        coVerify(exactly = 0) { deactivateUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN inactive status on first emission WHEN model created THEN deactivates protocol`() = runTest {
        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        coVerify { deactivateUseCase(token, SOURCE_ADDRESS) }
        coVerify(exactly = 0) { activateUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN missing network address WHEN status emitted THEN protocol status not sent`() = runTest {
        // Arrange — a Loading value carries no network address, so the side-effect must short-circuit
        stubStatus(CryptoCurrencyStatus(currency = token, value = CryptoCurrencyStatus.Loading).some())

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { activateUseCase(any(), any(), any()) }
        coVerify(exactly = 0) { deactivateUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN latest status loaded WHEN onStartEarningClick THEN pushes yield entry route`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        val routeSlot = slot<AppRoute>()

        // Act
        model.onStartEarningClick()

        // Assert
        verify { appRouter.push(capture(routeSlot), any()) }
        val route = routeSlot.captured as AppRoute.YieldSupplyEntry
        assertThat(route.userWalletId).isEqualTo(userWalletId)
        assertThat(route.cryptoCurrency).isEqualTo(token)
        assertThat(route.apy).isEqualTo("5")
    }

    @Test
    fun `GIVEN processing state WHEN onStartEarningClick THEN pushes route with empty apy`() = runTest {
        // Arrange — Processing state has no apy field, so the route apy collapses to empty
        coEvery { enterStatusUseCase(any(), any()) } returns YieldSupplyPendingStatus.Enter(txIds = listOf("0x1")).right()
        val model = createModel()
        advanceUntilIdle()
        val routeSlot = slot<AppRoute>()

        // Act
        model.onStartEarningClick()

        // Assert
        verify { appRouter.push(capture(routeSlot), any()) }
        assertThat((routeSlot.captured as AppRoute.YieldSupplyEntry).apy).isEmpty()
    }

    @Test
    fun `GIVEN no latest status WHEN onActiveClick THEN does not navigate`() = runTest {
        // Arrange — currency status never resolves, so latestCryptoCurrencyStatus stays null
        stubStatus(none())
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onActiveClick()

        // Assert
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @Test
    fun `GIVEN latest status loaded WHEN onLearnMoreClick THEN pushes stories route`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        val routeSlot = slot<AppRoute>()

        // Act
        model.onLearnMoreClick()

        // Assert
        verify { appRouter.push(capture(routeSlot), any()) }
        val route = routeSlot.captured as AppRoute.Stories
        assertThat(route.storyId).isEqualTo(StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id)
        assertThat(route.screenSource).isEqualTo("TokenDetails")
        assertThat(route.nextScreen).isInstanceOf(AppRoute.YieldSupplyEntry::class.java)
    }

    private fun stubStatus(option: Option<CryptoCurrencyStatus>, list: AccountStatusList = accountStatusList) {
        every {
            with(CryptoCurrencyStatusOperations) { list.getCryptoCurrencyStatus(any<CryptoCurrency>()) }
        } returns option
    }

    private fun TestScope.createModel(currency: CryptoCurrency = token): YieldSupplyModel = YieldSupplyModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyComponent.Params(userWalletId = userWalletId, cryptoCurrency = currency),
        ),
        dispatchers = createDispatchers(),
        analyticsEventsHandler = analytics,
        appRouter = appRouter,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = accountStatusListSupplier,
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        yieldSupplyGetTokenStatusUseCase = getTokenStatusUseCase,
        yieldSupplyIsAvailableUseCase = isAvailableUseCase,
        yieldSupplyActivateUseCase = activateUseCase,
        yieldSupplyDeactivateUseCase = deactivateUseCase,
        yieldSupplyEnterStatusUseCase = enterStatusUseCase,
        yieldSupplyEnterStatusFlowUseCase = enterStatusFlowUseCase,
        yieldSupplyMinAmountUseCase = minAmountUseCase,
        yieldSupplyGetDustMinAmountUseCase = getDustMinAmountUseCase,
        isYieldBoostPromoEnabledForTokenUseCase = isBoostPromoEnabledUseCase,
        getBoostedApyUseCase = getBoostedApyUseCase,
        boostStoryPreloader = boostStoryPreloader,
    )

    private fun TestScope.createDispatchers(): TestingCoroutineDispatcherProvider {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = dispatcher,
            mainImmediate = dispatcher,
            io = dispatcher,
            default = dispatcher,
            single = dispatcher,
        )
    }

    private fun status(
        currency: CryptoCurrency = token,
        isActive: Boolean = false,
        isAllowedToSpend: Boolean = true,
        amount: BigDecimal = BigDecimal.TEN,
        effectiveProtocolBalance: BigDecimal? = BigDecimal.ONE,
        fiatRate: BigDecimal? = BigDecimal.ONE,
        networkSource: StatusSource = StatusSource.ACTUAL,
        address: String = SOURCE_ADDRESS,
    ): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Custom(
            amount = amount,
            fiatAmount = amount,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = isActive,
                isInitialized = true,
                isAllowedToSpend = isAllowedToSpend,
                effectiveProtocolBalance = effectiveProtocolBalance,
            ),
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = address, type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(networkSource = networkSource),
        ),
    )

    private fun marketToken(isActive: Boolean): YieldMarketToken = YieldMarketToken(
        tokenAddress = "0xToken",
        chainId = 1,
        apy = BigDecimal("5"),
        isActive = isActive,
        maxFeeNative = BigDecimal.ZERO,
        maxFeeUSD = BigDecimal.ZERO,
        backendId = "ethereum",
    )

    private fun token(): CryptoCurrency.Token = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        ),
        network = network(),
        name = "TEST_TOKEN",
        symbol = "TTK",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xToken",
    )

    private fun coin(): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        ),
        network = network(),
        name = "TEST_COIN",
        symbol = "ETH",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

    private fun network(): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private companion object {
        const val SOURCE_ADDRESS = "0x1111111111111111111111111111111111111111"
    }
}