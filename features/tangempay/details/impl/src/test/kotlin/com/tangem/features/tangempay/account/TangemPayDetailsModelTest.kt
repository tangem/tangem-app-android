package com.tangem.features.tangempay.account

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.domain.pay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.pay.usecase.GetCashbackDeactivationDismissedUseCase
import com.tangem.domain.pay.usecase.GetCashbackSummaryUseCase
import com.tangem.domain.pay.usecase.ObserveCashbackSummaryUseCase
import com.tangem.domain.pay.usecase.SetCashbackDeactivationDismissedUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.pay.model.CardIssueOffers
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.pay.usecase.bankofframp.FindActiveBankOfframpOrderUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.account.cashback.TangemPayCashbackController
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayDetailsModelTest {

    private val userWalletId = UserWalletId("123")

    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk(relaxed = true)
    private val getCashbackSummaryUseCase: GetCashbackSummaryUseCase = mockk(relaxed = true)
    private val observeCashbackSummaryUseCase: ObserveCashbackSummaryUseCase = mockk()
    private val getCashbackDeactivationDismissedUseCase: GetCashbackDeactivationDismissedUseCase = mockk()
    private val setCashbackDeactivationDismissedUseCase: SetCashbackDeactivationDismissedUseCase = mockk(relaxed = true)
    private val cachedSummary = MutableStateFlow<CashbackSummary?>(null)
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase = mockk(relaxed = true)
    private val getCustomerOffers: GetCustomerOffersUseCase = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository = mockk(relaxed = true)
    private val findActiveBankOfframpOrderUseCase: FindActiveBankOfframpOrderUseCase = mockk()
    private val cashbackControllerFactory = object : TangemPayCashbackController.Factory {
        override fun create(scope: CoroutineScope, accountStatus: StateFlow<AccountStatus.Payment>) =
            TangemPayCashbackController(
                scope = scope,
                accountStatus = accountStatus,
                observeCashbackSummaryUseCase = observeCashbackSummaryUseCase,
                getCashbackSummaryUseCase = getCashbackSummaryUseCase,
                getCashbackDeactivationDismissedUseCase = getCashbackDeactivationDismissedUseCase,
                setCashbackDeactivationDismissedUseCase = setCashbackDeactivationDismissedUseCase,
                analytics = analytics,
                router = router,
            )
    }

    @BeforeEach
    fun resetCashbackMocks() {
        clearMocks(
            analytics,
            router,
            tangemPayFeatureToggles,
            getCashbackSummaryUseCase,
            observeCashbackSummaryUseCase,
            getCashbackDeactivationDismissedUseCase,
            setCashbackDeactivationDismissedUseCase,
            getBankCredentialsUseCase,
            sendFeedbackEmailUseCase,
            tangemPayWithdrawRepository,
            findActiveBankOfframpOrderUseCase,
        )
        coEvery { findActiveBankOfframpOrderUseCase(any()) } returns null.right()
        cachedSummary.value = null
        every { observeCashbackSummaryUseCase(any()) } returns cachedSummary
        coEvery { getCashbackDeactivationDismissedUseCase(any()) } returns false
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Cashback {

        @Test
        fun `GIVEN cached summary WHEN status loaded THEN widget shown without a refresh`() = runTest {
            // Arrange
            cachedSummary.value = enabledCashbackSummary()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cashbackBlockState).isInstanceOf(CashbackBlockUM.Widget::class.java)
            coVerify(exactly = 0) { getCashbackSummaryUseCase(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN cached widget WHEN status re-emits THEN widget survives and no refresh is sent`() = runTest {
            // Arrange
            cachedSummary.value = enabledCashbackSummary()
            val statusFlow = MutableStateFlow(paymentStatus(loadedStatus()))
            val model = createModel(testScope = this, statusFlow = statusFlow)
            advanceUntilIdle()

            // Act
            statusFlow.value = paymentStatus(loadedStatus(availableForWithdrawal = BigDecimal.TEN))
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cashbackBlockState).isInstanceOf(CashbackBlockUM.Widget::class.java)
            coVerify(exactly = 0) { getCashbackSummaryUseCase(any()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @EnumSource(RefreshTrigger::class)
        fun `GIVEN nothing cached WHEN refresh triggered THEN summary requested once and widget shown`(
            trigger: RefreshTrigger,
        ) = runTest {
            // Arrange
            givenRefreshReturns(enabledCashbackSummary().right())
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            trigger.fire(model)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cashbackBlockState).isInstanceOf(CashbackBlockUM.Widget::class.java)
            coVerify(exactly = 1) { getCashbackSummaryUseCase(userWalletId) }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN cached summary WHEN status is not loaded THEN cashback hidden`(model: NotLoadedStatusModel) =
            runTest {
                // Arrange
                cachedSummary.value = enabledCashbackSummary()

                // Act
                val payModel = createModel(testScope = this, statusValue = model.status)
                advanceUntilIdle()

                // Assert
                assertThat(payModel.uiState.value.cashbackBlockState).isNull()
                payModel.onDestroy()
            }

        @Test
        fun `GIVEN cached widget on inactive account WHEN account becomes loaded THEN widget and impression shown`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledCashbackSummary()
                val statusFlow = MutableStateFlow(paymentStatus(inactiveStatus()))
                val model = createModel(testScope = this, statusValue = inactiveStatus(), statusFlow = statusFlow)
                advanceUntilIdle()

                // Act
                statusFlow.value = paymentStatus(loadedStatus())
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.cashbackBlockState).isInstanceOf(CashbackBlockUM.Widget::class.java)
                verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerShowed>()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN cached alt_block summary WHEN status re-emitted THEN one cashback item under current plan`() =
            runTest {
                // Arrange
                cachedSummary.value = enabledCashbackSummary(displayMode = CashbackDisplayMode.ALT_BLOCK)
                val statusFlow = MutableStateFlow(paymentStatus(loadedStatus(planState = tariffPlanState())))
                val model = createModel(testScope = this, planState = tariffPlanState(), statusFlow = statusFlow)
                advanceUntilIdle()

                // Act
                statusFlow.value = paymentStatus(
                    loadedStatus(availableForWithdrawal = BigDecimal.TEN, planState = tariffPlanState()),
                )
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.cashbackBlockState).isNull()
                assertThat(menuTitleIds(model)).containsExactly(
                    R.string.tangempay_current_plan_title,
                    R.string.tangempay_cashback_menu_item_title,
                    R.string.tangempay_pay_support,
                ).inOrder()
                model.onDestroy()
            }

        @Test
        fun `GIVEN nothing cached WHEN refresh fails THEN error block shown`() = runTest {
            // Arrange
            givenRefreshReturns(VisaApiError.Unspecified.left())
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onStart()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cashbackBlockState).isInstanceOf(CashbackBlockUM.Error::class.java)
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            NotLoadedStatusModel(name = "inactive", status = inactiveStatus()),
            NotLoadedStatusModel(name = "deactivated", status = deactivatedStatus(id = "customer-id")),
        )
    }

    internal enum class RefreshTrigger(val fire: (TangemPayDetailsModel) -> Unit) {
        SCREEN_STARTED({ it.onStart() }),
        PULLED_TO_REFRESH({ it.onRefreshSwipe(ShowRefreshState(true)) }),
    }

    internal data class NotLoadedStatusModel(val name: String, val status: PaymentAccountStatusValue) {
        override fun toString(): String = name
    }

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

    @Test
    fun `GIVEN card issue failed WHEN screen started THEN failure is shown and the stack is not replaced`() = runTest {
        // Arrange
        val model = createModel(testScope = this, statusValue = cardIssueFailedStatus())
        advanceUntilIdle()

        // Act
        model.onStart()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.messageBanner?.state?.title)
            .isEqualTo(resourceReference(R.string.tangempay_failed_to_issue_card))
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN dismissed banner WHEN the failure status re-emits THEN the banner stays hidden`() = runTest {
        // Arrange
        val statusFlow = MutableStateFlow(paymentStatus(cardIssueFailedStatus()))
        val model = createModel(testScope = this, statusFlow = statusFlow)
        advanceUntilIdle()

        // Act
        model.onCardIssueFailedBannerDismissed()
        statusFlow.value = paymentStatus(cardIssueFailedStatus())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.messageBanner).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN dismissed banner WHEN the account recovers and fails again THEN the banner reappears`() = runTest {
        // Arrange
        val statusFlow = MutableStateFlow(paymentStatus(cardIssueFailedStatus()))
        val model = createModel(testScope = this, statusFlow = statusFlow)
        advanceUntilIdle()
        model.onCardIssueFailedBannerDismissed()

        // Act
        statusFlow.value = paymentStatus(loadedStatus())
        advanceUntilIdle()
        statusFlow.value = paymentStatus(cardIssueFailedStatus())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.messageBanner).isNotNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN card issue failed WHEN generic support clicked THEN feedback is sent for the failed account`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this, statusValue = cardIssueFailedStatus())
            advanceUntilIdle()

            // Act
            model.onContactSupportClicked()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                sendFeedbackEmailUseCase.invoke(
                    type = FeedbackEmailType.Visa.FeatureIsBeta(
                        walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                        customerId = "customer-id",
                    ),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN a status without a customer id WHEN generic support clicked THEN no feedback is sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this, statusValue = awaitingPlanSelectionStatus())
        advanceUntilIdle()

        // Act
        model.onContactSupportClicked()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { sendFeedbackEmailUseCase.invoke(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN card issue failed WHEN support clicked THEN failed issue feedback is sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this, statusValue = cardIssueFailedStatus())
        advanceUntilIdle()

        // Act
        model.onCardIssueFailedSupportClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            sendFeedbackEmailUseCase.invoke(
                type = FeedbackEmailType.Visa.FailedIssueCard(
                    walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                    customerId = "customer-id",
                ),
            )
        }
        model.onDestroy()
    }

    private fun cardIssueFailedStatus(planState: TangemPayTariffPlanState? = tariffPlanState()) =
        PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "customer-id", tariffPlan = planState)

    private fun inactiveStatus() = PaymentAccountStatusValue.Inactive(
        source = StatusSource.ACTUAL,
        fiatBalance = PaymentAccountStatusValue.FiatBalance(availableBalance = BigDecimal.ZERO, currency = "USD"),
        tariffPlan = tariffPlanState(),
    )

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

    @Test
    fun `GIVEN other networks sheet WHEN dismissed THEN choose network is reopened instead of closing the slot`() =
        runTest {
            // Arrange
            // Both sheets share one slot, so "Other networks" replaces its opener; closing it must not drop the
            // user out to the account screen.
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()

            // Act
            model.onSelectDisabled()
            model.onOtherNetworksDismiss()

            // Assert
            assertThat(openedSheets.filterNotNull()).containsExactly(
                TangemPayDetailsNavigation.OtherNetworks,
                TangemPayDetailsNavigation.ChooseNetwork(walletId = userWalletId),
            ).inOrder()
            model.onDestroy()
        }

    @Test
    fun `GIVEN deactivated account WHEN add funds clicked THEN add funds sheet opened`() = runTest {
        // Arrange
        val model = createModel(testScope = this, statusValue = deactivatedStatus(id = "customer-id"))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClickAddFunds()

        // Assert
        assertThat(openedSheets.filterIsInstance<TangemPayDetailsNavigation.AddFunds>()).hasSize(1)
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank offramp disabled WHEN withdraw clicked THEN swap is pushed without hub`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isBankOfframpEnabled } returns false
        val model = createModel(testScope = this, availableForWithdrawal = BigDecimal.TEN)
        advanceUntilIdle()

        // Act
        model.onClickWithdraw()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(match<AppRoute.Swap> { it.accountFlow == AccountFlow.Withdraw }) }
        verify(exactly = 0) { router.push(TangemPayAccountDetailsInnerRoute.WithdrawHub) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank offramp enabled WHEN withdraw clicked THEN hub screen pushed and swap not pushed`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isBankOfframpEnabled } returns true
        val model = createModel(testScope = this, availableForWithdrawal = BigDecimal.TEN)
        advanceUntilIdle()

        // Act
        model.onClickWithdraw()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.WithdrawHub) }
        verify(exactly = 0) { router.push(ofType<AppRoute.Swap>()) }
        coVerify(exactly = 0) { tangemPayWithdrawRepository.hasWithdrawOrder(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank offramp channel missing WHEN withdraw clicked THEN swap pushed instead of the hub`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isBankOfframpEnabled } returns true
        val model = createModel(
            testScope = this,
            availableForWithdrawal = BigDecimal.TEN,
            isBankOfframpAvailable = false,
        )
        advanceUntilIdle()

        // Act
        model.onClickWithdraw()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(TangemPayAccountDetailsInnerRoute.WithdrawHub) }
        verify(exactly = 1) { router.push(ofType<AppRoute.Swap>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank offramp channel not known yet WHEN withdraw clicked THEN swap pushed instead of the hub`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isBankOfframpEnabled } returns true
            val model = createModel(
                testScope = this,
                availableForWithdrawal = BigDecimal.TEN,
                isBankOfframpAvailable = null,
            )
            advanceUntilIdle()

            // Act
            model.onClickWithdraw()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.push(TangemPayAccountDetailsInnerRoute.WithdrawHub) }
            verify(exactly = 1) { router.push(ofType<AppRoute.Swap>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN an active bank offramp order WHEN withdraw clicked THEN in progress error shown and nothing pushed`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isBankOfframpEnabled } returns true
            coEvery { findActiveBankOfframpOrderUseCase(any()) } returns bankOfframpOrder().right()
            val model = createModel(testScope = this, availableForWithdrawal = BigDecimal.TEN)
            advanceUntilIdle()

            // Act
            model.onClickWithdraw()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
            verify(exactly = 0) { router.push(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the active order lookup fails WHEN withdraw clicked THEN hub pushed without in progress error`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isBankOfframpEnabled } returns true
            coEvery { findActiveBankOfframpOrderUseCase(any()) } returns VisaApiError.Unspecified.left()
            val model = createModel(testScope = this, availableForWithdrawal = BigDecimal.TEN)
            advanceUntilIdle()

            // Act
            model.onClickWithdraw()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.WithdrawHub) }
            verify(exactly = 0) { uiMessageSender.send(any<BottomSheetMessage>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN active withdrawal and bank offramp disabled WHEN withdraw clicked THEN in progress error shown and nothing pushed`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isBankOfframpEnabled } returns false
            coEvery { tangemPayWithdrawRepository.hasWithdrawOrder(any()) } returns true
            val model = createModel(testScope = this, availableForWithdrawal = BigDecimal.TEN)
            advanceUntilIdle()

            // Act
            model.onClickWithdraw()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
            verify(exactly = 0) { router.push(any()) }
            model.onDestroy()
        }

    private fun SlotNavigation<TangemPayDetailsNavigation>.trackSlot(): List<TangemPayDetailsNavigation?> {
        val tracked = mutableListOf<TangemPayDetailsNavigation?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    @Test
    fun `GIVEN a delivering card WHEN the status re-emits THEN the in-transit banner event is sent once`() = runTest {
        // Arrange
        val statusFlow = MutableStateFlow(paymentStatus(loadedStatus(cardState = TangemPayCardState.Delivering)))
        val model = createModel(testScope = this, statusFlow = statusFlow)
        advanceUntilIdle()

        // Act
        statusFlow.value = paymentStatus(loadedStatus(cardState = TangemPayCardState.Delivering))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardInTransitBannerShowed>())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN an active card WHEN it starts delivering THEN the in-transit banner event is sent`() = runTest {
        // Arrange
        val statusFlow = MutableStateFlow(paymentStatus(loadedStatus()))
        val model = createModel(testScope = this, statusFlow = statusFlow)
        advanceUntilIdle()
        verify(exactly = 0) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardInTransitBannerShowed>())
        }

        // Act
        statusFlow.value = paymentStatus(loadedStatus(cardState = TangemPayCardState.Delivering))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.CardInTransitBannerShowed>())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the delivery banner WHEN Activate card is tapped THEN the banner button event is sent`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onActivateCardClick(cardId = "card_1")

        // Assert
        verify(exactly = 1) {
            analytics.send(ofType<TangemPayAnalyticsEvents.Plastic.ActivateCardBannerButtonClicked>())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the offers request fails WHEN add card clicked THEN a message is shown and nothing is opened`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isPlasticCardOrderEnabled } returns true
            coEvery { getCustomerOffers.cardIssueOffers(any()) } returns VisaApiError.ServerUnavailable.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onAddCardClick(tariffState = null)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
            verify(exactly = 0) { router.push(any<TangemPayAccountDetailsInnerRoute.OrderCard>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN no offers WHEN add card clicked THEN a message is shown and nothing is opened`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isPlasticCardOrderEnabled } returns true
        coEvery { getCustomerOffers.cardIssueOffers(any()) } returns
            CardIssueOffers(virtual = null, plastic = null).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAddCardClick(tariffState = null)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<BottomSheetMessage>()) }
        verify(exactly = 0) { router.push(any<TangemPayAccountDetailsInnerRoute.OrderCard>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN only a virtual offer WHEN add card clicked THEN the order screen is opened`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isPlasticCardOrderEnabled } returns true
        coEvery { getCustomerOffers.cardIssueOffers(any()) } returns
            CardIssueOffers(virtual = virtualOffer(), plastic = null).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAddCardClick(tariffState = null)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.OrderCard()) }
        verify(exactly = 0) { uiMessageSender.send(any<BottomSheetMessage>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN only a plastic offer WHEN add card clicked THEN the order screen is opened`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isPlasticCardOrderEnabled } returns true
        coEvery { getCustomerOffers.cardIssueOffers(any()) } returns
            CardIssueOffers(virtual = null, plastic = plasticOffer()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onAddCardClick(tariffState = null)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.OrderCard()) }
        verify(exactly = 0) { uiMessageSender.send(any<BottomSheetMessage>()) }
        model.onDestroy()
    }

    private val availableNetwork = PaymentNetworkStatus.Available(
        network = mockk(relaxed = true),
        depositAddress = "0xDEPOSIT",
        chainId = 137L,
        cryptoCurrencyStatuses = emptyList(),
    )

    private fun plasticOffer() = Offer(
        type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
        fee = Offer.Fee(amount = BigDecimal("5.00"), currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = "spec", orderType = OrderType.UNKNOWN),
    )

    private fun virtualOffer() = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
        fee = Offer.Fee(amount = BigDecimal("5.00"), currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = "spec", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN),
    )

    private fun createModel(
        testScope: TestScope,
        statusSource: StatusSource = StatusSource.ACTUAL,
        availableForWithdrawal: BigDecimal = BigDecimal.ZERO,
        accountError: PaymentAccountStatusValue.Error? = null,
        statusValue: PaymentAccountStatusValue? = null,
        virtualAccount: VirtualAccountOnramp? = null,
        isBankOfframpAvailable: Boolean? = true,
        planState: TangemPayTariffPlanState? = null,
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
                isBankOfframpAvailable = isBankOfframpAvailable,
                planState = planState,
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
            getBalanceHidingSettingsUseCase = mockk(relaxed = true),
            uiMessageSender = uiMessageSender,
            txHistoryUpdateListener = mockk(relaxed = true),
            tangemPayWithdrawRepository = tangemPayWithdrawRepository,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            tangemPayFeatureToggles = tangemPayFeatureToggles,
            paymentAccountStatusFetcher = mockk(relaxed = true),
            produceTangemPayInitialDataUseCase = mockk(relaxed = true),
            onboardingRepository = mockk(relaxed = true),
            getCustomerOffers = getCustomerOffers,
            getBankCredentialsUseCase = getBankCredentialsUseCase,
            findActiveBankOfframpOrderUseCase = findActiveBankOfframpOrderUseCase,
            cashbackControllerFactory = cashbackControllerFactory,
            tangemPayCurrencyFactory = mockk(relaxed = true),
        )
    }

    private fun bankOfframpOrder(id: String = "order-1"): Order = Order(
        id = id,
        customerId = "customer-id",
        type = OrderType.BANK_OFFRAMP_RAIN,
        status = OrderStatus.PROCESSING,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private fun enabledCashbackSummary(
        displayMode: CashbackDisplayMode = CashbackDisplayMode.FULL,
    ) = CashbackSummary.Enabled(
        displayMode = displayMode,
        cashback = TangemPayCashback(
            confirmedAmount = BigDecimal("2.70"),
            totalEarnedAmount = BigDecimal("2.70"),
            currency = "USD",
            payoutCurrency = "USDC",
            previousPayout = null,
            period = TangemPayCashback.Period(
                year = 2026,
                month = 8,
                payoutStart = DateTime.parse("2026-09-04"),
                payoutEnd = DateTime.parse("2026-09-08"),
            ),
        ),
    )

    private fun givenRefreshReturns(result: Either<VisaApiError, CashbackSummary>) {
        coEvery { getCashbackSummaryUseCase(any()) } coAnswers {
            result.onRight { summary ->
                cachedSummary.value = summary.takeIf {
                    it is CashbackSummary.Enabled || it is CashbackSummary.Deactivated
                }
            }
        }
    }

    private fun menuTitleIds(model: TangemPayDetailsModel): List<Int> =
        model.uiState.value.topBarConfig.items.mapNotNull { (it.title as? TextReference.Res)?.id }

    private fun loadedStatus(
        statusSource: StatusSource = StatusSource.ACTUAL,
        accountError: PaymentAccountStatusValue.Error? = null,
        availableForWithdrawal: BigDecimal = BigDecimal.ZERO,
        virtualAccount: VirtualAccountOnramp? = null,
        isBankOfframpAvailable: Boolean? = true,
        cardState: TangemPayCardState = TangemPayCardState.Active,
        planState: TangemPayTariffPlanState? = null,
    ): PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        if (planState != null) every { tariffPlan } returns planState
        every { source } returns statusSource
        every { error } returns accountError
        every { customerId } returns "customer-id"
        every { paymentAccountAddress } returns "address"
        every { networks } returns listOf(availableNetwork())
        every { this@mockk.availableForWithdrawal } returns availableForWithdrawal
        every { this@mockk.virtualAccount } returns virtualAccount
        every { this@mockk.isBankOfframpAvailable } returns isBankOfframpAvailable
        every { cards } returns listOf(tangemPayCard(state = cardState))
        every { balance } returns PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = BigDecimal.ZERO,
                currency = "USD",
            ),
        )
    }

    private fun paymentStatus(value: PaymentAccountStatusValue): AccountStatus.Payment = mockk(relaxed = true) {
        every { this@mockk.value } returns value
        every { account } returns mockk(relaxed = true) {
            every { userWalletId } returns this@TangemPayDetailsModelTest.userWalletId
        }
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

        const val PRODUCT_INSTANCE_ID = "pi_account"
        const val DEPOSIT_ADDRESS = "address"

        val DEEPLINK_PLAN_STATE: TangemPayTariffPlanState = tariffPlanState()

        val BANK_CREDENTIALS = BankCredentials(
            type = "SWIFT",
            beneficiaryName = "Tangem",
            beneficiaryAddress = "Address",
            beneficiaryBankName = "Bank",
            beneficiaryBankAddress = "Bank address",
            accountNumber = "123456",
            routingNumber = "654321",
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

        private fun availableNetwork(): PaymentNetworkStatus.Available = mockk(relaxed = true)

        private fun deactivatedStatus(id: String): PaymentAccountStatusValue.Deactivated = mockk(relaxed = true) {
            every { source } returns StatusSource.ACTUAL
            every { customerId } returns id
            every { networks } returns listOf(
                PaymentNetworkStatus.Available(
                    network = mockk(relaxed = true),
                    depositAddress = "0xDEPOSIT",
                    chainId = 137L,
                    cryptoCurrencyStatuses = emptyList(),
                ),
            )
            every { networks } returns listOf(availableNetwork())
            every { balance } returns PaymentAccountStatusValue.Balance(
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal.ZERO,
                    currency = "USD",
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeepLinkInitialRoute {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN deeplink screen route WHEN status loaded THEN the requested screen is pushed once`(
            model: DeepLinkRouteModel,
        ) = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isPlasticCardOrderEnabled } returns true
            val payModel = createModel(
                testScope = this,
                initialRoute = model.initialRoute,
                planState = DEEPLINK_PLAN_STATE,
                statusEmissions = 2,
            )

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.push(route = model.expectedRoute, onComplete = any()) }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.Cashback.BannerClicked>()) }
            payModel.onDestroy()
        }

        @Test
        fun `GIVEN va onramp deeplink route WHEN status loaded THEN deposit sheet opens without a click event`() =
            runTest {
                // Arrange
                val payModel = createModel(
                    testScope = this,
                    initialRoute = TangemPayDetailsInitialRoute.VA_ONRAMP,
                    virtualAccount = VirtualAccountOnramp.Eligible,
                )
                val openedSheets = payModel.bottomSheetNavigation.trackSlot()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(openedSheets.filterIsInstance<TangemPayDetailsNavigation.VirtualAccountDeposit>())
                    .containsExactly(expectedDepositSheet())
                verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.VaTopupButtonClicked>()) }
                payModel.onDestroy()
            }

        @Test
        fun `GIVEN va onramp details deeplink route WHEN credentials load THEN requisites sheet opens`() = runTest {
            // Arrange
            coEvery { getBankCredentialsUseCase(any(), any()) } returns BANK_CREDENTIALS.right()
            val payModel = createModel(
                testScope = this,
                initialRoute = TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS,
                virtualAccount = VirtualAccountOnramp.Available(productInstanceId = PRODUCT_INSTANCE_ID),
            )
            val openedSheets = payModel.bottomSheetNavigation.trackSlot()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(openedSheets.filterIsInstance<TangemPayDetailsNavigation.VirtualAccountRequisites>())
                .containsExactly(
                    TangemPayDetailsNavigation.VirtualAccountRequisites(
                        userWalletId = userWalletId,
                        bankCredentials = BANK_CREDENTIALS,
                    ),
                )
            coVerify(exactly = 1) { getBankCredentialsUseCase(userWalletId, PRODUCT_INSTANCE_ID) }
            payModel.onDestroy()
        }

        @Test
        fun `GIVEN va onramp details deeplink route WHEN credentials fail THEN the details error sheet opens`() =
            runTest {
                // Arrange
                coEvery { getBankCredentialsUseCase(any(), any()) } returns mockk<VisaApiError>(relaxed = true).left()
                val payModel = createModel(
                    testScope = this,
                    initialRoute = TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS,
                    virtualAccount = VirtualAccountOnramp.Available(productInstanceId = PRODUCT_INSTANCE_ID),
                )
                val openedSheets = payModel.bottomSheetNavigation.trackSlot()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(openedSheets.filterIsInstance<TangemPayDetailsNavigation.VaBankingDetailsError>())
                    .containsExactly(
                        TangemPayDetailsNavigation.VaBankingDetailsError(
                            userWalletId = userWalletId,
                            productInstanceId = PRODUCT_INSTANCE_ID,
                        ),
                    )
                payModel.onDestroy()
            }

        @Test
        fun `GIVEN va onramp details deeplink route WHEN account is not eligible yet THEN deposit sheet opens`() =
            runTest {
                // Arrange
                val payModel = createModel(
                    testScope = this,
                    initialRoute = TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS,
                    virtualAccount = VirtualAccountOnramp.Eligible,
                )
                val openedSheets = payModel.bottomSheetNavigation.trackSlot()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(openedSheets.filterIsInstance<TangemPayDetailsNavigation.VirtualAccountDeposit>())
                    .containsExactly(expectedDepositSheet())
                coVerify(exactly = 0) { getBankCredentialsUseCase(any(), any()) }
                payModel.onDestroy()
            }

        private fun expectedDepositSheet() = TangemPayDetailsNavigation.VirtualAccountDeposit(
            virtualAccountOnramp = VirtualAccountOnramp.Eligible,
            userWalletId = userWalletId,
            paymentAccountAddress = DEPOSIT_ADDRESS,
        )

        private fun provideTestModels() = listOf(
            DeepLinkRouteModel(
                initialRoute = TangemPayDetailsInitialRoute.CASHBACK,
                expectedRoute = TangemPayAccountDetailsInnerRoute.Cashback,
            ),
            DeepLinkRouteModel(
                initialRoute = TangemPayDetailsInitialRoute.ORDER_CARD,
                expectedRoute = TangemPayAccountDetailsInnerRoute.OrderCard(),
            ),
            DeepLinkRouteModel(
                initialRoute = TangemPayDetailsInitialRoute.CURRENT_PLAN,
                expectedRoute = TangemPayAccountDetailsInnerRoute.CurrentPlan(DEEPLINK_PLAN_STATE),
            ),
            DeepLinkRouteModel(
                initialRoute = TangemPayDetailsInitialRoute.CHANGE_PLAN,
                expectedRoute = TangemPayAccountDetailsInnerRoute.SelectPlan(
                    tariffPlan = DEEPLINK_PLAN_STATE.tariff,
                    source = TangemPaySelectPlanSource.CHANGE_PLAN,
                ),
            ),
        )
    }

    internal data class DeepLinkRouteModel(
        val initialRoute: TangemPayDetailsInitialRoute,
        val expectedRoute: TangemPayAccountDetailsInnerRoute,
    ) {
        override fun toString(): String = initialRoute.name
    }
}