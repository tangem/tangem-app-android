package com.tangem.features.send.subcomponents.amount.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.resourceReference
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
import com.tangem.features.send.impl.R
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
 * Guards the route-decoupling fix: `SendAmountModel.configAmountNavigation()` filters its route flow on
 * the public `AmountRoute` interface, not the `internal CommonSendRoute.Amount`. The test feeds a
 * foreign `AmountRoute` (which is NOT a `CommonSendRoute.Amount`) and asserts the navigation result is
 * still produced — before the fix the `combine`'s `filterIsInstance<CommonSendRoute.Amount>()` dropped
 * it and `onNavigationResult` never fired, leaving an external host (e.g. staking) with a dead Next
 * button. Also checks the `isEditMode` → back-icon / primary-button mapping is unaffected.
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
            callback,
        )
        // No wallet → the model stays on AmountState.Empty (the heavy AmountStateConverter path is skipped),
        // which is all the navigation block needs to emit.
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
        // Cancel modelScope so the long-lived navigation/status collectors stop between tests.
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN a foreign AmountRoute WHEN model created THEN navigation produced with close icon and next button`() =
        runTest {
            // Arrange — a route that is NOT CommonSendRoute.Amount (the impl type the model used to filter on).
            val navSlot = slot<NavigationUM>()

            // Act
            createModel(testScope = this, route = TestAmountRoute(isEditMode = false))
            advanceUntilIdle()

            // Assert — before the fix this never fired for a non-CommonSendRoute.Amount route.
            verify(atLeast = 1) { callback.onNavigationResult(capture(navSlot)) }
            val content = navSlot.captured as NavigationUM.Content
            assertThat(content.backIconRes).isEqualTo(R.drawable.ic_close_24)
            assertThat(content.primaryButton.textReference).isEqualTo(resourceReference(R.string.common_next))
        }

    @Test
    fun `GIVEN a foreign AmountRoute in edit mode WHEN model created THEN navigation has back icon and continue button`() =
        runTest {
            // Arrange
            val navSlot = slot<NavigationUM>()

            // Act
            createModel(testScope = this, route = TestAmountRoute(isEditMode = true))
            advanceUntilIdle()

            // Assert
            verify(atLeast = 1) { callback.onNavigationResult(capture(navSlot)) }
            val content = navSlot.captured as NavigationUM.Content
            assertThat(content.backIconRes).isEqualTo(R.drawable.ic_back_24)
            assertThat(content.primaryButton.textReference).isEqualTo(resourceReference(R.string.common_continue))
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
            currentRoute = MutableStateFlow(route),
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