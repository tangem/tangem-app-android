package com.tangem.features.swap.v2.impl.amount.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.usecase.GetSwapQuoteUseCase
import com.tangem.domain.swap.usecase.SelectInitialPairUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.swap.v2.impl.amount.DefaultSwapAmountUpdateTrigger
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponent
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponentParams
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.store.SwapBestRateAnimationStore
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.DefaultSwapChooseTokenNetworkTrigger
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.expressProvider
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapAmountModelTest {

    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase = mockk()
    private val selectInitialPairUseCase: SelectInitialPairUseCase = mockk()
    private val getSwapQuoteUseCase: GetSwapQuoteUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getUserCountryUseCase: GetUserCountryUseCase = mockk()
    private val swapBestRateAnimationStore: SwapBestRateAnimationStore = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase = mockk()
    private val callback: SwapAmountComponent.ModelCallback = mockk(relaxed = true)

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val userWallet = MockUserWalletFactory.create()
    private val bitcoinStatus = loadedStatus(currencyFactory.createCoin(Blockchain.Bitcoin))
    private val ethereumStatus = loadedStatus(currencyFactory.createCoin(Blockchain.Ethereum))
    private val ethereumSwapCurrency = SwapCryptoCurrency(
        currencyStatus = ethereumStatus,
        providers = listOf(expressProvider().copy(rateTypes = listOf(ExpressRateType.Float))),
    )
    private val swapCurrencies = SwapCurrencies(
        fromGroup = SwapCurrenciesGroup(
            available = listOf(ethereumSwapCurrency),
            unavailable = emptyList(),
            isAfterSearch = false,
        ),
        toGroup = SwapCurrenciesGroup(available = emptyList(), unavailable = emptyList(), isAfterSearch = false),
    )

    private val swapAmountUpdateTrigger = DefaultSwapAmountUpdateTrigger()
    private val swapChooseTokenNetworkTrigger = DefaultSwapChooseTokenNetworkTrigger()

    /** Keeps `initCurrencies` suspended, as a slow wallet-manager creation does on device. */
    private var minimumAmountResult = CompletableDeferred<Either<Throwable, BigDecimal?>>()

    @BeforeEach
    fun setUp() {
        clearMocks(
            getMinimumTransactionAmountSyncUseCase,
            selectInitialPairUseCase,
            getSwapQuoteUseCase,
            getSelectedAppCurrencyUseCase,
            getUserCountryUseCase,
            swapBestRateAnimationStore,
            getWalletsUseCase,
            getCurrencyUSDQuoteUseCase,
            callback,
        )
        minimumAmountResult = CompletableDeferred()
        coEvery { getMinimumTransactionAmountSyncUseCase(any(), any()) } coAnswers { minimumAmountResult.await() }
        coEvery { selectInitialPairUseCase(any(), any(), any(), any(), any()) } returns ethereumSwapCurrency
        coEvery {
            getSwapQuoteUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns ExpressError.UnknownError.left()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        every { getUserCountryUseCase.invokeSync() } returns UserCountry.fromDeviceLocale().right()
        coEvery { swapBestRateAnimationStore.getSyncOrNull() } returns false
        every { getWalletsUseCase.invokeSync() } returns listOf(userWallet)
        coEvery { getCurrencyUSDQuoteUseCase(any()) } returns BigDecimal.ONE
    }

    /**
     * Driven by a hand-held scheduler instead of `runTest`: the model's quote polling is an endless
     * `PeriodicTask` loop, and `runTest`'s terminal `advanceUntilIdle()` would spin it forever.
     */
    @Test
    fun `GIVEN amount handed off while state is Empty WHEN receive token selected THEN amount applied and quoted`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val scope = TestScope(StandardTestDispatcher(scheduler))
        val model = createModel(testScope = scope)
        scheduler.runCurrent()

        // Act
        scope.launch {
            swapChooseTokenNetworkTrigger.trigger(
                swapCurrencies = swapCurrencies,
                cryptoCurrency = ethereumStatus.currency,
                shouldResetNavigation = false,
            )
        }
        scheduler.runCurrent()
        scope.launch {
            swapAmountUpdateTrigger.triggerUpdateAmount(amountValue = "0.001", isEnterInFiatSelected = false)
        }
        scheduler.runCurrent()
        minimumAmountResult.complete(BigDecimal.ZERO.right())
        scheduler.runCurrent()

        // Assert
        assertThat(model.uiState.value.enteredPrimaryAmount()).isEqualTo("0.001")
        coVerify(exactly = 1) {
            getSwapQuoteUseCase(
                userWallet = any(),
                fromCryptoCurrency = bitcoinStatus.currency,
                toCryptoCurrency = ethereumStatus.currency,
                amount = match { it.compareTo(BigDecimal("0.001")) == 0 },
                amountType = any(),
                rateType = any(),
                provider = any(),
            )
        }
        model.onDestroy()
    }

    private fun SwapAmountUM.enteredPrimaryAmount(): String? {
        val field = (primaryAmount as? SwapAmountFieldUM.Content)?.amountField as? AmountState.Data
        return field?.amountTextField?.value
    }

    private fun createModel(testScope: TestScope): SwapAmountModel {
        val params = SwapAmountComponentParams.AmountParams(
            amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
            analyticsCategoryName = CommonSendAnalyticEvents.SEND_CATEGORY,
            userWallet = userWallet,
            isBalanceHidingFlow = MutableStateFlow(false),
            swapDirection = SwapDirection.Direct,
            primaryCryptoCurrencyStatusFlow = MutableStateFlow(bitcoinStatus),
            secondaryCryptoCurrency = null,
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.SendWithSwap,
            accountFlow = MutableStateFlow(null),
            isAccountModeFlow = MutableStateFlow(false),
            title = TextReference.EMPTY,
            callback = callback,
            route = SendWithSwapRoute.Amount(isEditMode = false),
            filterProviderTypes = emptyList(),
            onCloseFlow = {},
        )
        return SwapAmountModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getMinimumTransactionAmountSyncUseCase = getMinimumTransactionAmountSyncUseCase,
            selectInitialPairUseCase = selectInitialPairUseCase,
            getSwapQuoteUseCase = getSwapQuoteUseCase,
            swapChooseTokenNetworkListener = swapChooseTokenNetworkTrigger,
            getAllowanceInfoUseCase = mockk(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getUserCountryUseCase = getUserCountryUseCase,
            swapBestRateAnimationStore = swapBestRateAnimationStore,
            router = mockk(relaxed = true),
            swapAmountAlertFactory = mockk(relaxed = true),
            swapAlertFactory = mockk(relaxed = true),
            swapAmountUpdateListener = swapAmountUpdateTrigger,
            swapAmountReduceListener = swapAmountUpdateTrigger,
            feeSelectorReloadTrigger = mockk(relaxed = true),
            shouldShowNotificationUseCase = mockk(),
            analyticsEventHandler = mockk(relaxed = true),
            getWalletsUseCase = getWalletsUseCase,
            getCurrencyUSDQuoteUseCase = getCurrencyUSDQuoteUseCase,
        )
    }

    private fun loadedStatus(currency: CryptoCurrency): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                NetworkAddress.Address(value = "address", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )
}