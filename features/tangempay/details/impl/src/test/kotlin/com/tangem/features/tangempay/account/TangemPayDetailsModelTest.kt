package com.tangem.features.tangempay.account

import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.addFundsButton
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
import com.tangem.features.tangempay.withdrawButton
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayDetailsModelTest {

    private val userWalletId = UserWalletId("123")

    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)

    @ParameterizedTest
    @MethodSource("provideMutedCases")
    fun `GIVEN status source WHEN status loaded THEN balance is muted only when cached`(case: MutedCase) = runTest {
        // Arrange + Act
        val model = createModel(
            testScope = this,
            statusSource = case.statusSource,
            availableForWithdrawal = BigDecimal.ZERO,
            accountError = case.accountError,
        )
        advanceUntilIdle()

        // Assert
        val balanceState = model.uiState.value.balanceBlockState
        assertThat(balanceState).isInstanceOf(TangemPayDetailsBalanceBlockState.Content::class.java)
        assertThat((balanceState as TangemPayDetailsBalanceBlockState.Content).isMuted).isEqualTo(case.expectedMuted)
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideTransactionClickCases")
    fun `GIVEN status WHEN transaction clicked THEN opens details only when customerId present`(
        case: TransactionClickCase,
    ) = runTest {
        // Arrange
        val model = createModel(testScope = this, statusValue = case.status)
        advanceUntilIdle()

        // Act
        model.onTransactionClick(mockk<TangemPayTxHistoryItem.Payment>(relaxed = true))

        // Assert
        verify(exactly = if (case.expectedOpened) 1 else 0) {
            analytics.send(ofType<TangemPayAnalyticsEvents.TransactionInListClicked>())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN virtual account is processing WHEN bank transfer clicked THEN preparation popup event sent`() =
        runTest {
            // GIVEN
            val model = createModel(testScope = this, virtualAccount = VirtualAccountOnramp.Processing)
            advanceUntilIdle()

            // WHEN
            model.onClickBankTransfer()

            // THEN
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaPreparationPopupShowed>()) }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.VaTopupButtonClicked>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded status WHEN banking details error shown THEN details error event sent`() = runTest {
        // GIVEN
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // WHEN
        model.showVaBankingDetailsError(productInstanceId = "pi_account")

        // THEN
        verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.VaDetailsErrorShowed>()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideInitialAddFundsCases")
    fun `GIVEN initial route WHEN status loaded twice THEN add funds opened only for ADD_FUNDS and only once`(
        case: InitialAddFundsCase,
    ) = runTest {
        // GIVEN
        // The status flow keeps emitting while the screen is alive, so two emissions pin that the sheet
        // requested by the top-up push is opened once and not reopened on every refresh.
        val model = createModel(testScope = this, initialRoute = case.initialRoute, statusEmissions = 2)
        val openedSheets = model.bottomSheetNavigation.trackSlot()

        // WHEN
        advanceUntilIdle()

        // THEN
        assertThat(openedSheets.filterIsInstance<TangemPayDetailsNavigation.AddFunds>())
            .hasSize(case.expectedAddFundsSheets)
        // The push path opens the sheet directly, so the "user tapped Add funds" event must not be sent
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.AddFundsClicked>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN plan is awaited WHEN screen started THEN plan selection replaces the stack`() = runTest {
        // GIVEN
        val awaitingPlanSelection = awaitingPlanSelectionStatus()
        val model = createModel(testScope = this, statusValue = awaitingPlanSelection)
        advanceUntilIdle()

        // WHEN
        model.onStart()
        advanceUntilIdle()

        // THEN
        verify(exactly = 1) { router.replaceAll(routes = selectPlanRoutes(awaitingPlanSelection), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN screen is stopped WHEN plan becomes awaited THEN stack is not replaced`() = runTest {
        // GIVEN
        val statusFlow = MutableStateFlow(paymentStatus(loadedStatus()))
        val model = createModel(testScope = this, statusFlow = statusFlow)
        model.onStart()
        advanceUntilIdle()
        // the plan flow is pushed on top of the details screen
        model.onStop()

        // WHEN
        // canceling the Plus transition drops its order before the Basic one is created
        statusFlow.value = paymentStatus(awaitingPlanSelectionStatus())
        advanceUntilIdle()
        // the Basic order is created, the account has a plan again
        statusFlow.value = paymentStatus(loadedStatus())
        advanceUntilIdle()

        // THEN
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN plan stayed awaited while stopped WHEN screen started again THEN plan selection replaces the stack`() =
        runTest {
            // GIVEN
            val awaitingPlanSelection = awaitingPlanSelectionStatus()
            val statusFlow = MutableStateFlow(paymentStatus(loadedStatus()))
            val model = createModel(testScope = this, statusFlow = statusFlow)
            model.onStart()
            advanceUntilIdle()
            model.onStop()
            statusFlow.value = paymentStatus(awaitingPlanSelection)
            advanceUntilIdle()

            // WHEN
            model.onStart()
            advanceUntilIdle()

            // THEN
            verify(exactly = 1) {
                router.replaceAll(routes = selectPlanRoutes(awaitingPlanSelection), onComplete = any())
            }
            model.onDestroy()
        }

    private fun awaitingPlanSelectionStatus() = PaymentAccountStatusValue.AwaitingPlanSelection(
        source = StatusSource.ACTUAL,
        tariffPlan = customerTariffPlan(
            plan = tariffPlan(tierId = "BASIC", isBasicTier = true, fees = emptyList()),
            source = TangemPayCustomerTariffPlan.Source.DEFAULT,
        ),
    )

    private fun selectPlanRoutes(status: PaymentAccountStatusValue.AwaitingPlanSelection) = arrayOf(
        TangemPayAccountDetailsInnerRoute.SelectPlan(
            tariffPlan = status.tariffPlan,
            source = TangemPaySelectPlanSource.TIERS_ONBOARDING,
        ),
    )

    private fun SlotNavigation<TangemPayDetailsNavigation>.trackSlot(): List<TangemPayDetailsNavigation?> {
        val tracked = mutableListOf<TangemPayDetailsNavigation?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    private fun createModel(
        testScope: TestScope,
        statusSource: StatusSource = StatusSource.ACTUAL,
        availableForWithdrawal: BigDecimal = BigDecimal.ZERO,
        accountError: PaymentAccountStatusValue.Error? = null,
        statusValue: PaymentAccountStatusValue? = null,
        virtualAccount: VirtualAccountOnramp? = null,
        initialRoute: TangemPayDetailsInitialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
        statusEmissions: Int = 1,
        statusFlow: Flow<AccountStatus.Payment>? = null,
    ): TangemPayDetailsModel {
        val initialStatus = paymentStatus(
            value = statusValue ?: loadedStatus(
                statusSource = statusSource,
                accountError = accountError,
                availableForWithdrawal = availableForWithdrawal,
                virtualAccount = virtualAccount,
            ),
        )
        val params = TangemPayDetailsContainerComponent.Params(
            initialStatus = initialStatus,
            initialRoute = initialRoute,
        )

        val statuses = statusFlow ?: List(statusEmissions) { initialStatus }.asFlow()
        every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns statuses

        return TangemPayDetailsModel(
            paramsContainer = MutableParamsContainer(params),
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            analytics = analytics,
            router = router,
            urlOpener = mockk(relaxed = true),
            getBalanceHidingSettingsUseCase = mockk(relaxed = true),
            uiMessageSender = mockk(relaxed = true),
            txHistoryUpdateListener = mockk(relaxed = true),
            tangemPayWithdrawRepository = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            expressTransactionsEventListener = mockk(relaxed = true),
            tangemPayFeatureToggles = mockk(relaxed = true),
            paymentAccountStatusFetcher = mockk(relaxed = true),
            produceTangemPayInitialDataUseCase = mockk(relaxed = true),
            onboardingRepository = mockk(relaxed = true),
            getCustomerOffers = mockk(relaxed = true),
            getCashbackSummaryUseCase = mockk(relaxed = true),
            getCashbackDeactivationDismissedUseCase = mockk(relaxed = true),
            setCashbackDeactivationDismissedUseCase = mockk(relaxed = true),
            tangemPayCurrencyFactory = mockk(relaxed = true),
        )
    }

    private fun loadedStatus(
        statusSource: StatusSource = StatusSource.ACTUAL,
        accountError: PaymentAccountStatusValue.Error? = null,
        availableForWithdrawal: BigDecimal = BigDecimal.ZERO,
        virtualAccount: VirtualAccountOnramp? = null,
    ): PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns statusSource
        every { error } returns accountError
        every { customerId } returns "customer-id"
        every { depositAddress } returns "address"
        every { this@mockk.virtualAccount } returns virtualAccount
        every { cards } returns listOf(tangemPayCard())
        every { balance } returns PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = BigDecimal.ZERO,
                currency = "USD",
            ),
            cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                id = "id",
                chainId = 1L,
                depositAddress = "address",
                tokenContractAddress = "contract",
                balance = BigDecimal.ZERO,
            ),
            availableForWithdrawal = availableForWithdrawal,
        )
    }

    private fun paymentStatus(value: PaymentAccountStatusValue): AccountStatus.Payment = mockk(relaxed = true) {
        every { this@mockk.value } returns value
        every { account } returns mockk(relaxed = true) {
            every { userWalletId } returns this@TangemPayDetailsModelTest.userWalletId
        }
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

    internal data class FreezeCase(
        val statusSource: StatusSource,
        val frozenState: TangemPayCardFrozenState,
        val availableForWithdrawal: BigDecimal,
        val expectedAddFundsEnabled: Boolean,
        val expectedWithdrawEnabled: Boolean,
    )

    internal data class MutedCase(
        val statusSource: StatusSource,
        val expectedMuted: Boolean,
        val accountError: PaymentAccountStatusValue.Error? = null,
    )

    internal data class InitialAddFundsCase(
        val name: String,
        val initialRoute: TangemPayDetailsInitialRoute,
        val expectedAddFundsSheets: Int,
    ) {
        override fun toString(): String = name
    }

    internal data class TransactionClickCase(
        val name: String,
        val status: PaymentAccountStatusValue?,
        val expectedOpened: Boolean,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        @JvmStatic
        fun provideFreezeCases() = listOf(
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.ZERO,
                expectedAddFundsEnabled = true,
                expectedWithdrawEnabled = false,
            ),
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = true,
                expectedWithdrawEnabled = true,
            ),
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Frozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = false,
                expectedWithdrawEnabled = false,
            ),
            FreezeCase(
                statusSource = StatusSource.CACHE,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = false,
                expectedWithdrawEnabled = false,
            ),
        )

        @JvmStatic
        fun provideMutedCases() = listOf(
            MutedCase(statusSource = StatusSource.ACTUAL, expectedMuted = false),
            MutedCase(statusSource = StatusSource.CACHE, expectedMuted = true),
            MutedCase(statusSource = StatusSource.ONLY_CACHE, expectedMuted = true),
            // ACTUAL but errored is still not fresh -> muted (guards !isFresh, not just source != ACTUAL)
            MutedCase(
                statusSource = StatusSource.ACTUAL,
                accountError = PaymentAccountStatusValue.Error.Unavailable,
                expectedMuted = true,
            ),
        )

        @JvmStatic
        fun provideInitialAddFundsCases() = listOf(
            InitialAddFundsCase(
                name = "top-up push route -> sheet opened once",
                initialRoute = TangemPayDetailsInitialRoute.ADD_FUNDS,
                expectedAddFundsSheets = 1,
            ),
            InitialAddFundsCase(
                name = "default route -> sheet not opened",
                initialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
                expectedAddFundsSheets = 0,
            ),
            InitialAddFundsCase(
                name = "tiers route -> sheet not opened",
                initialRoute = TangemPayDetailsInitialRoute.TIERS_ONBOARDING,
                expectedAddFundsSheets = 0,
            ),
        )

        @JvmStatic
        fun provideTransactionClickCases() = listOf(
            TransactionClickCase(
                name = "deactivated account (has customerId) -> opens",
                status = deactivatedStatus(id = "customer-id"),
                expectedOpened = true,
            ),
            TransactionClickCase(
                name = "loaded account (has customerId) -> opens",
                status = null,
                expectedOpened = true,
            ),
            TransactionClickCase(
                name = "loading status (no customerId) -> ignored",
                status = PaymentAccountStatusValue.Loading,
                expectedOpened = false,
            ),
            TransactionClickCase(
                name = "not created status (no customerId) -> ignored",
                status = PaymentAccountStatusValue.NotCreated,
                expectedOpened = false,
            ),
        )

        private fun deactivatedStatus(id: String): PaymentAccountStatusValue.Deactivated = mockk(relaxed = true) {
            every { source } returns StatusSource.ACTUAL
            every { customerId } returns id
            every { balance } returns PaymentAccountStatusValue.Balance(
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal.ZERO,
                    currency = "USD",
                ),
                cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                    id = "id",
                    chainId = 1L,
                    depositAddress = "address",
                    tokenContractAddress = "contract",
                    balance = BigDecimal.ZERO,
                ),
                availableForWithdrawal = BigDecimal.ZERO,
            )
        }
    }
}