package com.tangem.features.tangempay.campaign

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.IssueAdditionalCardError
import com.tangem.domain.pay.model.IssueAdditionalCardResult
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.usecase.IssueAdditionalCardUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.components.TangemPayCampaignIssueCardComponent
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.tariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
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
import java.util.Currency

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayCampaignIssueCardModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val issueAdditionalCardUseCase: IssueAdditionalCardUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private var model: TangemPayCampaignIssueCardModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            issueAdditionalCardUseCase,
            paymentAccountStatusFetcher,
            paymentAccountStatusSupplier,
        )
        coEvery { paymentAccountStatusFetcher.invoke(WALLET_ID) } returns Unit.right()
        every { paymentAccountStatusSupplier.invoke(WALLET_ID) } returns flowOf(paymentStatus())
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN params WHEN model created THEN screen shows the art and an idle Issue button`() = runTest {
        // Act
        val model = createModel(testScope = this)

        // Assert
        val state = model.uiState.value
        assertThat(state.cardArtUrl).isEqualTo(CARD_ART_URL)
        assertThat(state.isIssuing).isFalse()
        assertThat(state.issueFee).contains("0")
    }

    @Nested
    inner class Issue {

        @Test
        fun `GIVEN order created WHEN issue clicked THEN Payment account opened`() = runTest {
            // Arrange
            coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                IssueAdditionalCardResult.Created(order()).right()
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onIssueClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.isIssuing).isTrue()
            coVerifyOrder {
                paymentAccountStatusFetcher.invoke(WALLET_ID)
                router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayDetails(status = paymentStatus()))
            }
        }

        @Test
        fun `GIVEN active order resumed WHEN issue clicked THEN Payment account opened`() = runTest {
            // Arrange
            coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                IssueAdditionalCardResult.Resumed(order()).right()
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onIssueClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayDetails(status = paymentStatus()))
            }
        }

        @Test
        fun `GIVEN issue in progress WHEN issue clicked THEN button loading AND request not repeated`() = runTest {
            // Arrange
            coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } coAnswers { awaitCancellation() }
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onIssueClick()
            model.uiState.value.onIssueClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.isIssuing).isTrue()
            coVerify(exactly = 1) { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) }
            verify { router wasNot Called }
        }

        @Test
        fun `GIVEN offer not available on basic plan WHEN issue clicked THEN cards limit for plan sheet shown`() =
            runTest {
                // Arrange
                coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                    IssueAdditionalCardError.OfferNotAvailable.left()
                every { paymentAccountStatusSupplier.invoke(WALLET_ID) } returns
                    flowOf(paymentStatus(planState = BASIC_PLAN_STATE))
                val message = captureMessage()
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onIssueClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.isIssuing).isFalse()
                assertThat(message.captured.sheetTitle())
                    .isEqualTo(resourceReference(R.string.tangempay_maximum_cards_issued_for_plan_title))
                verify { router wasNot Called }
            }

        @Test
        fun `GIVEN offer not available on paid plan WHEN issue clicked THEN cards limit sheet shown`() = runTest {
            // Arrange
            coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                IssueAdditionalCardError.OfferNotAvailable.left()
            every { paymentAccountStatusSupplier.invoke(WALLET_ID) } returns
                flowOf(paymentStatus(planState = tariffPlanState()))
            val message = captureMessage()
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onIssueClick()
            advanceUntilIdle()

            // Assert
            assertThat(message.captured.sheetTitle())
                .isEqualTo(resourceReference(R.string.tangempay_maximum_cards_issued_title))
        }

        @Test
        fun `GIVEN cards limit sheet WHEN upgrade plan clicked THEN current plan opened`() = runTest {
            // Arrange
            coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                IssueAdditionalCardError.OfferNotAvailable.left()
            val status = paymentStatus(planState = BASIC_PLAN_STATE)
            every { paymentAccountStatusSupplier.invoke(WALLET_ID) } returns flowOf(status)
            val message = captureMessage()
            val model = createModel(testScope = this)
            model.uiState.value.onIssueClick()
            advanceUntilIdle()

            // Act
            message.captured.clickLastButton()
            advanceUntilIdle()

            // Assert
            val currentPlanRoute = AppRoute.TangemPayDetails(
                status = status,
                initialRoute = TangemPayDetailsInitialRoute.CURRENT_PLAN,
            )
            verify(exactly = 1) { router.replaceAll(AppRoute.Wallet, currentPlanRoute) }
        }

        @Test
        fun `GIVEN no campaign offer WHEN issue clicked THEN unavailable dialog shown AND OK opens Payment account`() =
            runTest {
                // Arrange
                coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                    IssueAdditionalCardError.NoCampaignOffer.left()
                val message = captureMessage()
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onIssueClick()
                advanceUntilIdle()

                // Assert
                val dialog = message.captured as DialogMessage
                assertThat(dialog.title).isEqualTo(resourceReference(R.string.tangempay_campaign_unavailable_title))
                verify { router wasNot Called }

                // Act
                dialog.onDismissRequest()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayDetails(status = paymentStatus()))
                }
            }

        @Test
        fun `GIVEN other error WHEN issue clicked THEN snackbar shown AND screen stays`() = runTest {
            // Arrange
            coEvery { issueAdditionalCardUseCase(WALLET_ID, CAMPAIGN_KEY) } returns
                IssueAdditionalCardError.Other(VisaApiError.ServerUnavailable).left()
            val message = captureMessage()
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onIssueClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.isIssuing).isFalse()
            assertThat(message.captured).isEqualTo(
                SnackbarMessage(
                    message = resourceReference(R.string.common_unknown_error),
                    startIconId = R.drawable.ic_alert_circle_24,
                ),
            )
            verify { router wasNot Called }
        }
    }

    @Nested
    inner class Close {

        @Test
        fun `WHEN close THEN status refetched AND Payment account opened`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.onClose()
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                paymentAccountStatusFetcher.invoke(WALLET_ID)
                router.replaceAll(AppRoute.Wallet, AppRoute.TangemPayDetails(status = paymentStatus()))
            }
            coVerify(exactly = 0) { issueAdditionalCardUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN no payment account status WHEN close THEN wallet opened`() = runTest {
            // Arrange
            every { paymentAccountStatusSupplier.invoke(WALLET_ID) } returns emptyFlow()
            val model = createModel(testScope = this)

            // Act
            model.onClose()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        }
    }

    private fun captureMessage(): CapturingSlot<EventMessage> {
        val message = slot<EventMessage>()
        every { uiMessageSender.send(capture(message)) } returns Unit
        return message
    }

    private fun EventMessage.sheetTitle(): TextReference? {
        return (this as BottomSheetMessage).messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.InfoBlock>()
            .firstNotNullOfOrNull { it.title }
    }

    private fun EventMessage.clickLastButton() {
        val sheet = (this as BottomSheetMessage).messageBottomSheetUM
        sheet.elements.filterIsInstance<MessageBottomSheetUM.Button>().last().onClick?.invoke(sheet.closeScope)
    }

    private fun paymentStatus(planState: TangemPayTariffPlanState? = null): AccountStatus.Payment {
        return AccountStatus.Payment(
            account = Account.Payment(WALLET_ID),
            value = PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "customer", tariffPlan = planState),
        )
    }

    private fun order(): Order = Order(
        id = "order",
        customerId = "customer",
        type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
        status = OrderStatus.NEW,
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

    private fun createModel(testScope: TestScope): TangemPayCampaignIssueCardModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        return TangemPayCampaignIssueCardModel(
            paramsContainer = MutableParamsContainer(
                TangemPayCampaignIssueCardComponent.Params(
                    userWalletId = WALLET_ID,
                    campaignKey = CAMPAIGN_KEY,
                    cardArtUrl = CARD_ART_URL,
                    feeAmount = BigDecimal.ZERO,
                    feeCurrency = Currency.getInstance("USD"),
                ),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            router = router,
            uiMessageSender = uiMessageSender,
            issueAdditionalCardUseCase = issueAdditionalCardUseCase,
            paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        ).also { model = it }
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val CARD_ART_URL = "https://example.com/oxford.png"
        val BASIC_PLAN_STATE = tariffPlanState(
            tariff = customerTariffPlan(plan = tariffPlan(tierId = "BASIC", isBasicTier = true, fees = emptyList())),
        )
    }
}