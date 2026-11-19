package com.tangem.features.tangempay.withdraw.bank.preview.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.tangempay.model.bankofframp.BankAccountType
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.bankOfframpQuote
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.personalFormValues
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
import com.tangem.features.tangempay.withdraw.bank.preview.completion.BankWithdrawCompletion
import com.tangem.features.tangempay.withdraw.bank.preview.completion.BankWithdrawCompletionFactory
import com.tangem.features.tangempay.withdraw.bank.preview.completion.CompletionState
import com.tangem.features.tangempay.withdraw.bank.preview.completion.NewRecipientListener
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Failed
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RateLockUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RecipientCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Summary
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.speiDestination
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.THREE_STARS
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.time.CurrentTimeProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawPreviewModelTest : TangemPayModelTestBase() {

    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk {
        every { isMxnEnabled } returns true
    }
    private val userWalletId = UserWalletId("011")
    private val sourceAccount = BankWithdrawSourceAccount.Payment(userWalletId)
    private val completion: BankWithdrawCompletion = mockk()
    private val completionFactory: BankWithdrawCompletionFactory = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val onOrderCreated: (BankOfframpOrderResult) -> Unit = {}
    private val onDestinationChanged: (OfframpDestination) -> Unit = {}
    private val onNetworkFeeChanged: (BigDecimal) -> Unit = {}
    private val newRecipientListener: NewRecipientListener = mockk()
    private val shownQuotes = mutableListOf<BankOfframpQuote>()
    private val quote = MutableStateFlow<BankOfframpQuote?>(null)
    private val onCloseFlow: () -> Unit = mockk(relaxed = true)
    private var completedCount = 0
    private var outcomeUnknownCount = 0

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, completion, completionFactory, getUserWalletUseCase, analytics, onCloseFlow)
        completedCount = 0
        outcomeUnknownCount = 0
        shownQuotes.clear()
        every { getUserWalletUseCase(userWalletId) } returns mockk<UserWallet.Cold>().right()
        every { completionFactory.create(any(), any(), any()) } returns completion
        coEvery { completion.prepare(any(), any()) } returns Unit
        quote.value = null
        every { completion.quote } returns quote
        completionInFlight()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitialState {

        @Test
        fun `GIVEN complete draft WHEN model created THEN summary with transfer recipient cards and send enabled`() {
            // Act
            val model = createModel()

            // Assert
            val state = summary(model)
            assertThat(state.header.title).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_confirm_title))
            assertThat(state.transfer?.amount?.amount).isEqualTo("$500")
            assertThat(state.recipientCards.map { it.rows.size }).containsExactly(2, 7, 5).inOrder()
            assertThat(state.isSendEnabled).isTrue()
            assertThat(state.isSending).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN balance hidden WHEN model created THEN source balance masked`() {
            // Act
            val model = createModel(isBalanceHidden = true)

            // Assert
            assertThat(summary(model).transfer?.amount?.balance).isEqualTo(THREE_STARS)
            model.onDestroy()
        }

        @Test
        fun `GIVEN source account WHEN model created THEN completion created for it with the draft callbacks`() {
            // Arrange
            val callbacks = slot<BankWithdrawCompletionFactory.Callbacks>()

            // Act
            val model = createModel()

            // Assert
            verify(exactly = 1) {
                completionFactory.create(sourceAccount = sourceAccount, scope = any(), callbacks = capture(callbacks))
            }
            assertThat(callbacks.captured.onOrderCreated).isSameInstanceAs(onOrderCreated)
            assertThat(callbacks.captured.onDestinationChanged).isSameInstanceAs(onDestinationChanged)
            assertThat(callbacks.captured.onNetworkFeeChanged).isSameInstanceAs(onNetworkFeeChanged)
            assertThat(callbacks.captured.newRecipientListener).isSameInstanceAs(newRecipientListener)
            model.onDestroy()
        }

        @Test
        fun `GIVEN draft WHEN model created THEN completion prepares the terms of that draft`() {
            // Arrange
            val draft = savedRecipientDraft()

            // Act
            val model = createModel(draft = draft)

            // Assert
            coVerify(exactly = 1) { completion.prepare(draft, any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN recipient not loaded WHEN model created THEN send disabled`() {
            // Act
            val model = createModel(draft = savedRecipientDraft(recipient = null))

            // Assert
            assertThat(summary(model).isSendEnabled).isFalse()
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN wallet lookup result WHEN model created THEN summary marks the nfc wallet unless the wallet is hot`(
            testModel: WalletLookupModel,
        ) {
            // Arrange
            every { getUserWalletUseCase(userWalletId) } returns testModel.lookupResult

            // Act
            val model = createModel()

            // Assert
            assertThat(summary(model).isColdWallet).isEqualTo(testModel.isColdWallet)
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            WalletLookupModel(name = "cold wallet", lookupResult = mockk<UserWallet.Cold>().right(), isColdWallet = true),
            WalletLookupModel(name = "hot wallet", lookupResult = mockk<UserWallet.Hot>().right(), isColdWallet = false),
            WalletLookupModel(
                name = "wallet lookup fails, icon kept",
                lookupResult = GetUserWalletError.UserWalletNotFound.left(),
                isColdWallet = true,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Send {

        @Test
        fun `GIVEN complete draft WHEN send THEN completion started with the draft and sending stays on`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            coVerify(exactly = 1) { completion.complete(savedRecipientDraft(), any()) }
            assertThat(summary(model).isSending).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN send in flight WHEN send again THEN completion started once`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).onSendClick()
            summary(model).onSendClick()

            // Assert
            coVerify(exactly = 1) { completion.complete(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN general failure WHEN send THEN failed screen with retry`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(PreviewFailure.General))
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            val failed = model.uiState.value as Failed
            assertThat(failed.title).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_error_general_title))
            assertThat(failed.onRetryClick).isNotNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN failure WHEN retry THEN completion started again`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(PreviewFailure.General))
            val model = createModel()
            summary(model).onSendClick()

            // Act
            requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

            // Assert
            coVerify(exactly = 2) { completion.complete(savedRecipientDraft(), any()) }
            assertThat(model.uiState.value).isInstanceOf(Failed::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN withdrawal in progress WHEN send twice THEN sheet shown each time and summary kept`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.WithdrawalInProgress)
            val model = createModel()

            // Act
            summary(model).onSendClick()
            val afterFirst = summary(model)
            summary(model).onSendClick()

            // Assert
            verify(exactly = 2) { uiMessageSender.send(any()) }
            assertThat(afterFirst.isSending).isFalse()
            coVerify(exactly = 2) { completion.complete(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN completion completed WHEN send THEN completion reported and sending ends`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Completed)
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            assertThat(completedCount).isEqualTo(1)
            assertThat(summary(model).isSending).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN card prompt aborted WHEN completion idle again THEN summary editable again`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Idle)
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            assertThat(completedCount).isEqualTo(0)
            val state = summary(model)
            assertThat(state.isSending).isFalse()
            assertThat(state.isSendEnabled).isTrue()
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideOutcomeUnknownModels")
        fun `GIVEN failure WHEN send THEN source account refresh requested only for an unknown transfer outcome`(
            testModel: OutcomeUnknownModel,
        ) {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(testModel.failure))
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            assertThat(outcomeUnknownCount).isEqualTo(testModel.expectedRefreshRequests)
            assertThat(completedCount).isEqualTo(0)
            model.onDestroy()
        }

        private fun provideOutcomeUnknownModels() = listOf(
            OutcomeUnknownModel(failure = PreviewFailure.General, expectedRefreshRequests = 0),
            OutcomeUnknownModel(failure = PreviewFailure.TransferOutcomeUnknown, expectedRefreshRequests = 1),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Navigation {

        @Test
        fun `GIVEN send in flight WHEN back or close THEN navigation ignored`() {
            // Arrange
            val model = createModel()
            summary(model).onSendClick()

            // Act
            summary(model).onBackClick()
            summary(model).onCloseClick()

            // Assert
            verify(exactly = 0) { router.pop(any()) }
            verify(exactly = 0) { onCloseFlow() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN summary WHEN back THEN router pops`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).onBackClick()

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN summary WHEN close THEN flow closed`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).onCloseClick()

            // Assert
            verify(exactly = 1) { onCloseFlow() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN failed WHEN close THEN flow closed`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(PreviewFailure.General))
            val model = createModel()
            summary(model).onSendClick()

            // Act
            (model.uiState.value as Failed).onCloseClick()

            // Assert
            verify(exactly = 1) { onCloseFlow() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN summary WHEN edit requested THEN edit route pushed`() {
            // Arrange
            val model = createModel()

            // Act
            (model.uiState.value as Summary).transfer?.amount?.onClick?.invoke()

            // Assert
            verify(exactly = 1) {
                router.push(TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount), any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN send in flight WHEN edit requested THEN nothing pushed`() {
            // Arrange
            val model = createModel()
            val summary = model.uiState.value as Summary
            summary.onSendClick()

            // Act
            (model.uiState.value as Summary).transfer?.amount?.onClick?.invoke()

            // Assert
            verify(exactly = 0) { router.push(any(), any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfframpAnalytics {

        @Test
        fun `GIVEN complete draft WHEN model created THEN ConfirmationScreenOpened is sent`() {
            // Act
            val model = createModel()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("recipientCardCases")
        fun `GIVEN new recipient WHEN recipient or bank card tapped THEN the card details event is sent`(
            testModel: RecipientCardTapModel,
        ) {
            // Arrange
            val model = createModel(draft = newRecipientDraft())

            // Act
            testModel.pickCard(summary(model).recipientCards).onClick?.invoke()

            // Assert
            verify(exactly = 1) { analytics.send(testModel.expectedEvent) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN summary WHEN amount card tapped THEN no offramp event is sent`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).transfer?.amount?.onClick?.invoke()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened()) }
            verify(exactly = 1) { analytics.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN send in flight WHEN edit requested THEN no offramp event is sent`() {
            // Arrange
            val model = createModel(draft = newRecipientDraft())
            val summary = summary(model)
            summary.onSendClick()

            // Act
            summary.recipientCards.first().onClick?.invoke()

            // Assert
            verify(exactly = 0) {
                analytics.send(TangemPayAnalyticsEvents.Offramp.RecipientDetailsOnConfirmationClicked())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN summary WHEN send tapped THEN SendOnConfirmationClicked is sent and the completion started`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()) }
            coVerify(exactly = 1) { completion.complete(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN send in flight WHEN send tapped again THEN SendOnConfirmationClicked is sent once`() {
            // Arrange
            val model = createModel()

            // Act
            summary(model).onSendClick()
            summary(model).onSendClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN failure WHEN send THEN SendErrorScreenOpened is sent`(failure: PreviewFailure) {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(failure))
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN failed send WHEN retry fails too THEN only the first tap reported and the error screen twice`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(PreviewFailure.General))
            val model = createModel()
            summary(model).onSendClick()

            // Act
            requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()) }
            verify(exactly = 2) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("endStatesWithoutFailure")
        fun `GIVEN send ends without a failure WHEN send THEN no SendErrorScreenOpened is sent`(endState: CompletionState) {
            // Arrange
            completionReports(CompletionState.Sending, endState)
            val model = createModel()

            // Act
            summary(model).onSendClick()

            // Assert
            verify(exactly = 0) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN new recipient WHEN rail row tapped THEN RailSelectorClicked is sent and the amount step opened`() {
            // Arrange
            val model = createModel(newRecipientDraft())

            // Act
            summary(model).transfer?.route?.onClick?.invoke()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked()) }
            verify(exactly = 1) {
                router.push(TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount), any())
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN send in flight WHEN rail row tapped THEN no RailSelectorClicked is sent`() {
            // Arrange
            val model = createModel(newRecipientDraft())
            val onRailClick = requireNotNull(summary(model).transfer?.route?.onClick)
            summary(model).onSendClick()

            // Act
            onRailClick()

            // Assert
            verify(exactly = 0) { analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked()) }
            verify(exactly = 0) {
                router.push(TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount), any())
            }
            model.onDestroy()
        }

        private fun provideTestModels() = PreviewFailure.entries

        private fun recipientCardCases() = listOf(
            RecipientCardTapModel(
                name = "recipient card",
                pickCard = { cards -> cards.first() },
                expectedEvent = TangemPayAnalyticsEvents.Offramp.RecipientDetailsOnConfirmationClicked(),
            ),
            RecipientCardTapModel(
                name = "bank card",
                pickCard = { cards -> cards.last() },
                expectedEvent = TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsOnConfirmationClicked(),
            ),
        )

        private fun endStatesWithoutFailure() = listOf(CompletionState.WithdrawalInProgress, CompletionState.Idle)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MoneyAccountAnalytics {

        @Test
        fun `GIVEN money account failure with an error code WHEN send THEN VA Send failed carries the code`() {
            // Arrange
            completionReports(
                CompletionState.Sending,
                CompletionState.Failed(
                    failure = PreviewFailure.UnsupportedConfiguration,
                    errorCode = FORBIDDEN_ERROR_CODE,
                ),
            )
            val model = createModel(
                sourceAccount = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1"),
            )

            // Act
            summary(model).onSendClick()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    MoneyAccountAnalyticsEvents.SendFailed(
                        errorCode = FORBIDDEN_ERROR_CODE,
                        method = MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_ACH,
                    ),
                )
            }
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents>()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideMoneyAccountSendModels")
        fun `GIVEN money account WHEN send attempts finish THEN send confirmed once per completed or failed attempt`(
            testModel: MoneyAccountSendModel,
        ) {
            // Arrange
            completionReportsInTurn(*testModel.attempts.toTypedArray())
            val sentEvents = mutableListOf<AnalyticsEvent>()
            every { analytics.send(capture(sentEvents)) } returns Unit
            val model = createModel(sourceAccount = MONEY_ACCOUNT)

            // Act
            testModel.actions.forEach { action -> action.perform(model) }

            // Assert
            val actual = sentEvents.filter { it.event in MONEY_ACCOUNT_SEND_EVENTS }.map { it.event }
            assertThat(actual).containsExactlyElementsIn(testModel.expectedEvents).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account WHEN send completes THEN send confirmed carries the draft amount and rail`() {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Completed)
            val model = createModel(sourceAccount = MONEY_ACCOUNT)

            // Act
            summary(model).onSendClick()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    MoneyAccountAnalyticsEvents.SendConfirmed(
                        amountUsdc = BigDecimal("500"),
                        method = MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_ACH,
                    ),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account WHEN preview opened and Pay tapped THEN both events carry the rail`() {
            // Arrange
            val model = createModel(sourceAccount = MONEY_ACCOUNT)

            // Act
            summary(model).onSendClick()

            // Assert
            verifyOrder {
                analytics.send(MoneyAccountAnalyticsEvents.OfframpConfirmationScreenOpened(rail = BankRail.ACH))
                analytics.send(MoneyAccountAnalyticsEvents.OfframpRecipientTransferConfirmed(rail = BankRail.ACH))
            }
            model.onDestroy()
        }

        private fun provideMoneyAccountSendModels() = listOf(
            MoneyAccountSendModel(
                name = "network fee requoted, then sent",
                attempts = listOf(
                    listOf(CompletionState.Sending, CompletionState.Idle),
                    listOf(CompletionState.Sending, CompletionState.Completed),
                ),
                actions = listOf(SendAction.PAY, SendAction.PAY),
                expectedEvents = listOf(TRANSFER_CONFIRMED, TRANSFER_CONFIRMED, SEND_CONFIRMED),
            ),
            MoneyAccountSendModel(
                name = "requoted twice, never sent",
                attempts = listOf(
                    listOf(CompletionState.Sending, CompletionState.Idle),
                    listOf(CompletionState.Sending, CompletionState.Idle),
                ),
                actions = listOf(SendAction.PAY, SendAction.PAY),
                expectedEvents = listOf(TRANSFER_CONFIRMED, TRANSFER_CONFIRMED),
            ),
            MoneyAccountSendModel(
                name = "failed, then retried and sent",
                attempts = listOf(
                    listOf(CompletionState.Sending, CompletionState.Failed(PreviewFailure.General)),
                    listOf(CompletionState.Sending, CompletionState.Completed),
                ),
                actions = listOf(SendAction.PAY, SendAction.RETRY),
                expectedEvents = listOf(TRANSFER_CONFIRMED, SEND_CONFIRMED, SEND_FAILED, SEND_CONFIRMED),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SpeiQuote {

        @Test
        fun `GIVEN quote held WHEN time passes THEN rate lock counts down to the send deadline`() = runTest {
            // Arrange
            val model = createSpeiModel()
            quote.value = bankOfframpQuote(expiresAt = NOW.plusMinutes(15))
            runCurrent()

            // Act
            advanceTimeBy(2_000)
            runCurrent()

            // Assert
            assertThat(summary(model).transfer?.conversion?.rateLock).isEqualTo(
                RateLockUM.Countdown(
                    text = resourceReference(R.string.money_account_send_rate_lock, wrappedList("14:28")),
                ),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN quote with exactly a minute to its send deadline WHEN it arrives THEN the lock counts down`() =
            runTest {
                // Arrange
                val model = createSpeiModel()

                // Act
                quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(90))
                runCurrent()

                // Assert
                assertThat(summary(model).transfer?.conversion?.rateLock).isEqualTo(
                    RateLockUM.Countdown(
                        text = resourceReference(R.string.money_account_send_rate_lock, wrappedList("01:00")),
                    ),
                )
                model.onDestroy()
            }

        @Test
        fun `GIVEN quote counting down WHEN a newer quote arrives THEN the lock follows only the newer one`() =
            runTest {
                // Arrange
                val model = createSpeiModel()
                quote.value = bankOfframpQuote(expiresAt = NOW.plusMinutes(15))
                runCurrent()
                quote.value = bankOfframpQuote(expiresAt = NOW.plusMinutes(5).plusMillis(500))
                runCurrent()

                // Act
                advanceTimeBy(2_000)
                runCurrent()

                // Assert
                assertThat(summary(model).transfer?.conversion?.rateLock).isEqualTo(
                    RateLockUM.Countdown(
                        text = resourceReference(R.string.money_account_send_rate_lock, wrappedList("04:29")),
                    ),
                )
                model.onDestroy()
            }

        @Test
        fun `GIVEN quote request pending WHEN the order changes THEN the pending request is cancelled`() = runTest {
            // Arrange
            val draftFlow = MutableStateFlow(speiDraft())
            var isFirstRequestCancelled = false
            coEvery { completion.prepare(speiDraft(), any()) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    isFirstRequestCancelled = true
                }
            }
            val model = createModel(
                sourceAccount = MONEY_ACCOUNT,
                draftFlow = draftFlow,
                dispatchers = createTestingCoroutineDispatcherProvider(),
            )
            runCurrent()

            // Act
            draftFlow.value = speiDraft().copy(amount = BigDecimal("600"))
            runCurrent()

            // Assert
            assertThat(isFirstRequestCancelled).isTrue()
            model.onDestroy()
        }

        @Test
        fun `GIVEN quote held WHEN its send deadline passes THEN a new quote is requested`() = runTest {
            // Arrange
            val model = createSpeiModel()
            quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
            runCurrent()

            // Act
            advanceTimeBy(62_000)
            runCurrent()

            // Assert
            coVerify(exactly = 2) { completion.prepare(speiDraft(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account quote held WHEN its send deadline passes THEN quote expired sent once`() = runTest {
            // Arrange
            val model = createSpeiModel()
            quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
            runCurrent()

            // Act
            advanceTimeBy(62_000)
            runCurrent()

            // Assert
            verify(exactly = 1) { analytics.send(quoteExpired()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN money account SPEI preview WHEN the transfer is refused on an expired quote THEN quote expired sent`() {
            // Arrange
            val callbacks = slot<BankWithdrawCompletionFactory.Callbacks>()
            every { completionFactory.create(any(), any(), capture(callbacks)) } returns completion
            val model = createModel(draft = speiDraft(), sourceAccount = MONEY_ACCOUNT)

            // Act
            callbacks.captured.onQuoteExpired()

            // Assert
            verify(exactly = 1) { analytics.send(quoteExpired()) }
            model.onDestroy()
        }

        private fun quoteExpired() = MoneyAccountAnalyticsEvents.OfframpAmountErrorShown(
            rail = BankRail.SPEI,
            reason = MoneyAccountAnalyticsEvents.AmountErrorReason.QUOTE_EXPIRED,
        )

        @ParameterizedTest
        @MethodSource("provideLateQuoteExpiries")
        fun `GIVEN quote late on arrival WHEN time passes THEN no countdown and no new quote`(expiresAt: DateTime) =
            runTest {
                // Arrange
                val model = createSpeiModel()

                // Act
                quote.value = bankOfframpQuote(expiresAt = expiresAt)
                runCurrent()
                advanceTimeBy(10_000)
                runCurrent()

                // Assert
                assertThat(summary(model).transfer?.conversion?.rateLock).isEqualTo(RateLockUM.Hidden)
                assertThat(summary(model).isSendEnabled).isTrue()
                coVerify(exactly = 1) { completion.prepare(speiDraft(), any()) }
                model.onDestroy()
            }

        private fun provideLateQuoteExpiries() = listOf(NOW.minusMinutes(5), NOW.plusSeconds(60))

        @Test
        fun `GIVEN lock runs out during a send WHEN the send ends without sending THEN a new quote is requested`() =
            runTest {
                // Arrange
                val tapCancelled = CompletableDeferred<Unit>()
                coEvery { completion.complete(any(), any()) } coAnswers {
                    val onState = secondArg<(CompletionState) -> Unit>()
                    onState(CompletionState.Sending)
                    tapCancelled.await()
                    onState(CompletionState.Idle)
                }
                val model = createSpeiModel()
                quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
                runCurrent()
                summary(model).onSendClick()
                advanceTimeBy(62_000)
                runCurrent()

                // Act
                tapCancelled.complete(Unit)
                runCurrent()

                // Assert
                coVerify(exactly = 2) { completion.prepare(speiDraft(), any()) }
                verify(exactly = 1) { analytics.send(quoteExpired()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN failure on screen WHEN the send deadline passes THEN no new quote is requested`() = runTest {
            // Arrange
            completionReports(CompletionState.Sending, CompletionState.Failed(PreviewFailure.General))
            val model = createSpeiModel()
            quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
            runCurrent()
            summary(model).onSendClick()
            runCurrent()

            // Act
            advanceTimeBy(62_000)
            runCurrent()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(Failed::class.java)
            coVerify(exactly = 1) { completion.prepare(any(), any()) }
            verify(exactly = 0) { analytics.send(quoteExpired()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN preview hidden under an edit step WHEN the send deadline passes THEN re-quoted with no quote expired`() =
            runTest {
                // Arrange
                val model = createSpeiModel()
                quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
                runCurrent()
                model.onScreenVisibilityChanged(isVisible = false)

                // Act
                advanceTimeBy(62_000)
                runCurrent()

                // Assert
                coVerify(exactly = 2) { completion.prepare(speiDraft(), any()) }
                verify(exactly = 0) { analytics.send(quoteExpired()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN quote expiry reported by the countdown WHEN the backend also refuses it as expired THEN sent once`() =
            runTest {
                // Arrange
                val callbacks = slot<BankWithdrawCompletionFactory.Callbacks>()
                every { completionFactory.create(any(), any(), capture(callbacks)) } returns completion
                val model = createSpeiModel()
                quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
                runCurrent()
                advanceTimeBy(62_000)
                runCurrent()

                // Act
                callbacks.captured.onQuoteExpired()

                // Assert
                verify(exactly = 1) { analytics.send(quoteExpired()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN countdown re-quote fails WHEN the send deadline passes THEN quote expired then quote unavailable`() =
            runTest {
                // Arrange
                val model = createSpeiModel()
                quote.value = bankOfframpQuote(expiresAt = NOW.plusSeconds(92))
                runCurrent()
                coEvery { completion.prepare(any(), any()) } coAnswers {
                    secondArg<(CompletionState) -> Unit>().invoke(CompletionState.Failed(PreviewFailure.General))
                }

                // Act
                advanceTimeBy(62_000)
                runCurrent()

                // Assert
                verifyOrder {
                    analytics.send(quoteExpired())
                    analytics.send(
                        MoneyAccountAnalyticsEvents.OfframpAmountErrorShown(
                            rail = BankRail.SPEI,
                            reason = MoneyAccountAnalyticsEvents.AmountErrorReason.QUOTE_UNAVAILABLE,
                        ),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN quote refused on open WHEN model created THEN error screen without send analytics`() {
            // Arrange
            coEvery { completion.prepare(any(), any()) } coAnswers {
                secondArg<(CompletionState) -> Unit>().invoke(CompletionState.Failed(PreviewFailure.General))
            }
            val sentEvents = mutableListOf<AnalyticsEvent>()
            every { analytics.send(capture(sentEvents)) } returns Unit

            // Act
            val model = createModel(draft = speiDraft(), sourceAccount = MONEY_ACCOUNT)

            // Assert
            assertThat(model.uiState.value).isInstanceOf(Failed::class.java)
            assertThat(sentEvents.map { it.event }).containsNoneIn(MONEY_ACCOUNT_SEND_EVENTS)
            model.onDestroy()
        }

        @Test
        fun `GIVEN quotes arrive WHEN preview open THEN every quote is handed to the wizard`() = runTest {
            // Arrange
            val model = createSpeiModel()
            runCurrent()

            // Act
            quote.value = bankOfframpQuote(quoteId = "quote-1")
            runCurrent()
            quote.value = bankOfframpQuote(quoteId = "quote-2")
            runCurrent()

            // Assert
            assertThat(shownQuotes.map { it.quoteId }).containsExactly("quote-1", "quote-2").inOrder()
            model.onDestroy()
        }

        @ParameterizedTest
        @MethodSource("provideSpeiSendModels")
        fun `GIVEN SPEI draft WHEN Pay ends THEN send confirmed and the outcome carry the SPEI method`(
            testModel: SpeiSendModel,
        ) {
            // Arrange
            completionReports(CompletionState.Sending, testModel.endState)
            val sentEvents = mutableListOf<AnalyticsEvent>()
            every { analytics.send(capture(sentEvents)) } returns Unit
            val model = createModel(draft = speiDraft(), sourceAccount = MONEY_ACCOUNT)

            // Act
            summary(model).onSendClick()

            // Assert
            assertThat(sentEvents.filter { it.event in MONEY_ACCOUNT_SEND_EVENTS }).containsExactlyElementsIn(
                testModel.expectedEvents,
            ).inOrder()
            model.onDestroy()
        }

        private fun provideSpeiSendModels() = listOf(
            SpeiSendModel(
                endState = CompletionState.Completed,
                expectedEvents = listOf(
                    MoneyAccountAnalyticsEvents.OfframpRecipientTransferConfirmed(rail = BankRail.SPEI),
                    MoneyAccountAnalyticsEvents.SendConfirmed(
                        amountUsdc = BigDecimal("500"),
                        method = MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_SPEI,
                    ),
                ),
            ),
            SpeiSendModel(
                endState = CompletionState.Failed(failure = PreviewFailure.General, errorCode = FORBIDDEN_ERROR_CODE),
                expectedEvents = listOf(
                    MoneyAccountAnalyticsEvents.OfframpRecipientTransferConfirmed(rail = BankRail.SPEI),
                    MoneyAccountAnalyticsEvents.SendConfirmed(
                        amountUsdc = BigDecimal("500"),
                        method = MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_SPEI,
                    ),
                    MoneyAccountAnalyticsEvents.SendFailed(
                        errorCode = FORBIDDEN_ERROR_CODE,
                        method = MoneyAccountAnalyticsEvents.SendMethod.BANK_TRANSFER_SPEI,
                    ),
                ),
            ),
        )

        @Test
        fun `GIVEN SPEI draft WHEN the quote fails after Pay THEN quote unavailable sent and no send events`() {
            // Arrange
            completionReports(
                CompletionState.Sending,
                CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104140168, isQuoteFailure = true),
            )
            val sentEvents = mutableListOf<AnalyticsEvent>()
            every { analytics.send(capture(sentEvents)) } returns Unit
            val model = createModel(draft = speiDraft(), sourceAccount = MONEY_ACCOUNT)

            // Act
            summary(model).onSendClick()

            // Assert
            assertThat(sentEvents.map { it.event }).containsNoneOf(SEND_CONFIRMED, SEND_FAILED)
            assertThat(sentEvents).contains(
                MoneyAccountAnalyticsEvents.OfframpAmountErrorShown(
                    rail = BankRail.SPEI,
                    reason = MoneyAccountAnalyticsEvents.AmountErrorReason.QUOTE_UNAVAILABLE,
                ),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN quote refused on open WHEN Retry fails again THEN each failure sent as quote unavailable`() {
            // Arrange
            coEvery { completion.prepare(any(), any()) } coAnswers {
                secondArg<(CompletionState) -> Unit>().invoke(CompletionState.Failed(PreviewFailure.General))
            }
            val model = createModel(draft = speiDraft(), sourceAccount = MONEY_ACCOUNT)

            // Act
            requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

            // Assert
            verify(exactly = 2) {
                analytics.send(
                    MoneyAccountAnalyticsEvents.OfframpAmountErrorShown(
                        rail = BankRail.SPEI,
                        reason = MoneyAccountAnalyticsEvents.AmountErrorReason.QUOTE_UNAVAILABLE,
                    ),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN quote refused on open WHEN Retry fails again THEN it re-quotes without send analytics`() {
            // Arrange
            coEvery { completion.prepare(any(), any()) } coAnswers {
                secondArg<(CompletionState) -> Unit>().invoke(CompletionState.Failed(PreviewFailure.General))
            }
            val sentEvents = mutableListOf<AnalyticsEvent>()
            every { analytics.send(capture(sentEvents)) } returns Unit
            val model = createModel(draft = speiDraft(), sourceAccount = MONEY_ACCOUNT)

            // Act
            requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(Failed::class.java)
            assertThat(sentEvents.map { it.event }).containsNoneIn(MONEY_ACCOUNT_SEND_EVENTS)
            coVerify(exactly = 2) { completion.prepare(speiDraft(), any()) }
            coVerify(exactly = 0) { completion.complete(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN preview open WHEN the draft changes THEN only a change of the order requests a new quote`() {
            // Arrange
            val draftFlow = MutableStateFlow(speiDraft())
            val model = createModel(sourceAccount = MONEY_ACCOUNT, draftFlow = draftFlow)
            val repricedDraft = speiDraft().copy(amount = BigDecimal("600"))

            // Act
            draftFlow.value = speiDraft().copy(networkFee = BigDecimal("0.07"))
            draftFlow.value = repricedDraft

            // Assert
            coVerifyOrder {
                completion.prepare(speiDraft(), any())
                completion.prepare(repricedDraft, any())
            }
            coVerify(exactly = 2) { completion.prepare(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN no quote yet WHEN it arrives THEN Pay turns on`() = runTest {
            // Arrange
            val model = createSpeiModel()
            runCurrent()
            val isEnabledBefore = summary(model).isSendEnabled

            // Act
            quote.value = bankOfframpQuote(expiresAt = NOW.plusMinutes(15))
            runCurrent()

            // Assert
            assertThat(isEnabledBefore).isFalse()
            assertThat(summary(model).isSendEnabled).isTrue()
            model.onDestroy()
        }

        private fun TestScope.createSpeiModel() = createModel(
            draft = speiDraft(),
            sourceAccount = MONEY_ACCOUNT,
            dispatchers = createTestingCoroutineDispatcherProvider(),
            currentTimeProvider = CurrentTimeProvider { NOW.millis + testScheduler.currentTime },
        )

        private fun speiDraft() = savedRecipientDraft().copy(
            destination = speiDestination(),
            networkFee = BigDecimal("0.03"),
        )

    }

    internal data class SpeiSendModel(val endState: CompletionState, val expectedEvents: List<AnalyticsEvent>)

    internal data class OutcomeUnknownModel(val failure: PreviewFailure, val expectedRefreshRequests: Int)

    data class WalletLookupModel(
        val name: String,
        val lookupResult: Either<GetUserWalletError, UserWallet>,
        val isColdWallet: Boolean,
    ) {
        override fun toString(): String = name
    }

    data class RecipientCardTapModel(
        val name: String,
        val pickCard: (List<RecipientCardUM>) -> RecipientCardUM,
        val expectedEvent: AnalyticsEvent,
    ) {
        override fun toString(): String = name
    }

    private fun completionInFlight() {
        coEvery { completion.complete(any(), any()) } coAnswers {
            secondArg<(CompletionState) -> Unit>().invoke(CompletionState.Sending)
            awaitCancellation()
        }
    }

    private fun completionReportsInTurn(vararg attempts: List<CompletionState>) {
        val remaining = ArrayDeque(attempts.toList())
        coEvery { completion.complete(any(), any()) } coAnswers {
            remaining.removeFirst().forEach(secondArg<(CompletionState) -> Unit>())
        }
    }

    data class MoneyAccountSendModel(
        val name: String,
        val attempts: List<List<CompletionState>>,
        val actions: List<SendAction>,
        val expectedEvents: List<String>,
    ) {
        override fun toString(): String = name
    }

    enum class SendAction {
        PAY,
        RETRY,
        ;

        fun perform(model: TangemPayBankWithdrawPreviewModel) = when (this) {
            PAY -> (model.uiState.value as Summary).onSendClick()
            RETRY -> requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()
        }
    }

    private fun completionReports(vararg states: CompletionState) {
        coEvery { completion.complete(any(), any()) } coAnswers {
            states.forEach(secondArg<(CompletionState) -> Unit>())
        }
    }

    private fun summary(model: TangemPayBankWithdrawPreviewModel): Summary = model.uiState.value as Summary

    private fun newRecipientDraft() = BankWithdrawDraft(
        entry = BankWithdrawEntry.NewPersonal,
        recipient = null,
        newRecipient = NewRecipientDraft(
            isThirdParty = false,
            accountType = BankAccountType.CHECKING,
            fields = personalFormValues,
        ),
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    @Suppress("LongParameterList")
    private fun createModel(
        draft: BankWithdrawDraft = savedRecipientDraft(),
        isBalanceHidden: Boolean = false,
        sourceAccount: BankWithdrawSourceAccount = this.sourceAccount,
        draftFlow: MutableStateFlow<BankWithdrawDraft> = MutableStateFlow(draft),
        dispatchers: TestingCoroutineDispatcherProvider = this.dispatchers,
        currentTimeProvider: CurrentTimeProvider = CurrentTimeProvider { NOW.millis },
    ): TangemPayBankWithdrawPreviewModel =
        TangemPayBankWithdrawPreviewModel(
            paramsContainer = MutableParamsContainer(
                TangemPayBankWithdrawPreviewComponent.Params(
                    sourceAccount = sourceAccount,
                    draft = draftFlow,
                    onOrderCreated = onOrderCreated,
                    onDestinationChanged = onDestinationChanged,
                    onNetworkFeeChanged = onNetworkFeeChanged,
                    newRecipientListener = newRecipientListener,
                    onQuoteShown = shownQuotes::add,
                    onCompleted = { completedCount++ },
                    onTransferOutcomeUnknown = { outcomeUnknownCount++ },
                    onCloseFlow = onCloseFlow,
                ),
            ),
            dispatchers = dispatchers,
            router = router,
            uiMessageSender = uiMessageSender,
            completionFactory = completionFactory,
            getUserWalletUseCase = getUserWalletUseCase,
            withdrawAnalyticsFactory = withdrawAnalyticsFactory(moneyAccountFeatureToggles),
            getBalanceHidingSettingsUseCase = mockk { every { isBalanceHidden() } returns flowOf(isBalanceHidden) },
            currentTimeProvider = currentTimeProvider,
        ).destroyAfterTest().also { it.onScreenVisibilityChanged(isVisible = true) }

    private companion object {
        const val FORBIDDEN_ERROR_CODE = 104_120_100
        val NOW: DateTime = DateTime.parse("2026-11-10T10:00:00Z")
        const val TRANSFER_CONFIRMED = "VA Offramp Recipient Transfer Confirmed"
        const val SEND_CONFIRMED = "VA Send Confirmed"
        const val SEND_FAILED = "VA Send failed"

        val MONEY_ACCOUNT = BankWithdrawSourceAccount.Money(UserWalletId("011"), productInstanceId = "instance-1")
        val MONEY_ACCOUNT_SEND_EVENTS = setOf(TRANSFER_CONFIRMED, SEND_CONFIRMED, SEND_FAILED)
    }
}