package com.tangem.features.send.subcomponents.amount.model

import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.amount.AmountRoute
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.api.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.api.subcomponents.amount.SendAmountUpdateListener
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Guards the route-decoupling fix at the model level. The amount step is now hostable by foreign flows
 * (e.g. staking / send-with-swap) that supply their own [AmountRoute] implementation rather than the
 * `internal CommonSendRoute.Amount`. The navigation chrome (back icon / Next vs Continue) moved out of
 * the model into `DefaultSendAmountComponent`, but the model still makes a route-driven decision in
 * [SendAmountModel.onConvertToAnotherToken]: it reads `params.route.isEditMode` *directly* (previously
 * it collected the route flow). This test feeds a foreign [AmountRoute] and verifies that edit-mode
 * branches to the "reset sending" alert while a fresh (non-edit) route converts immediately — both via
 * the public interface, not `CommonSendRoute`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SendAmountNavigationTest {

    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase = mockk()
    private val sendAmountReduceListener: SendAmountReduceListener = mockk()
    private val sendAmountUpdateListener: SendAmountUpdateListener = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val sendAmountAlertFactory: SendAmountAlertFactory = mockk(relaxed = true)
    private val getWalletsUseCase: GetWalletsUseCase = mockk(relaxed = true)

    private val callback: SendAmountComponent.ModelCallback = mockk(relaxed = true)
    private val cryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)

    private var model: SendAmountModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(
            getMinimumTransactionAmountSyncUseCase,
            sendAmountReduceListener,
            sendAmountUpdateListener,
            getSelectedAppCurrencyUseCase,
            getUserWalletUseCase,
            sendAmountAlertFactory,
            callback,
        )
        // No wallet → the model stays on AmountState.Empty (the heavy AmountStateConverter path is skipped),
        // which is all these route-driven assertions need.
        every { getUserWalletUseCase.invokeFlow(any()) } returns emptyFlow()
        every { sendAmountReduceListener.reduceToTriggerFlow } returns emptyFlow()
        every { sendAmountReduceListener.reduceByTriggerFlow } returns emptyFlow()
        every { sendAmountReduceListener.ignoreReduceTriggerFlow } returns emptyFlow()
        every { sendAmountUpdateListener.updateAmountTriggerFlow } returns emptyFlow()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { getMinimumTransactionAmountSyncUseCase(any(), any()) } returns BigDecimal.ZERO.right()
    }

    @AfterEach
    fun tearDown() {
        // Cancel modelScope so the long-lived reduce/status collectors stop between tests.
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN foreign AmountRoute in edit mode WHEN onConvertToAnotherToken THEN reset sending alert shown`() =
        runTest {
            // Arrange
            createModel(testScope = this, route = TestAmountRoute(isEditMode = true))
            advanceUntilIdle()

            // Act
            model?.onConvertToAnotherToken()
            advanceUntilIdle()

            // Assert — edit mode must guard the conversion behind the reset-sending confirmation.
            verify(exactly = 1) { sendAmountAlertFactory.showResetSendingAlert(any()) }
            verify(exactly = 0) { callback.onConvertToAnotherToken(any(), any()) }
        }

    @Test
    fun `GIVEN foreign AmountRoute not in edit mode WHEN onConvertToAnotherToken THEN converts without alert`() =
        runTest {
            // Arrange
            createModel(testScope = this, route = TestAmountRoute(isEditMode = false))
            advanceUntilIdle()

            // Act
            model?.onConvertToAnotherToken()
            advanceUntilIdle()

            // Assert — a fresh route converts immediately, with no reset-sending alert.
            verify(exactly = 0) { sendAmountAlertFactory.showResetSendingAlert(any()) }
            verify(exactly = 1) { callback.onConvertToAnotherToken(any(), any()) }
        }

    private fun createModel(testScope: TestScope, route: AmountRoute): SendAmountModel {
        val params = SendAmountComponentParams.AmountParams(
            state = AmountState.Empty,
            analyticsCategoryName = "test",
            userWalletId = UserWalletId(stringValue = "0123456789"),
            appCurrency = AppCurrency.Default,
            predefinedValues = PredefinedValues.Empty,
            cryptoCurrency = cryptoCurrency,
            cryptoCurrencyStatusFlow = MutableStateFlow(mockk<CryptoCurrencyStatus>(relaxed = true)),
            isBalanceHidingFlow = MutableStateFlow(false),
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
            accountFlow = MutableStateFlow<Account?>(null),
            isAccountModeFlow = MutableStateFlow(false),
            callback = callback,
            route = route,
        )
        return SendAmountModel(
            paramsContainer = MutableParamsContainer(value = params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getMinimumTransactionAmountSyncUseCase = getMinimumTransactionAmountSyncUseCase,
            sendAmountReduceListener = sendAmountReduceListener,
            sendAmountUpdateListener = sendAmountUpdateListener,
            analyticsEventHandler = analyticsEventHandler,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            feeSelectorReloadTrigger = feeSelectorReloadTrigger,
            getUserWalletUseCase = getUserWalletUseCase,
            sendAmountAlertFactory = sendAmountAlertFactory,
            getWalletsUseCase = getWalletsUseCase,
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

    private data class TestAmountRoute(override val isEditMode: Boolean) : AmountRoute
}