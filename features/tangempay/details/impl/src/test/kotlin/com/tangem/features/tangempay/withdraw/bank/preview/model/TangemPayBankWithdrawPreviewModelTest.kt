package com.tangem.features.tangempay.withdraw.bank.preview.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
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
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Failed
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Summary
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.THREE_STARS
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawPreviewModelTest {

    private val userWalletId = UserWalletId("011")
    private val sourceAccount = BankWithdrawSourceAccount.Payment(userWalletId)
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val completion: BankWithdrawCompletion = mockk()
    private val completionFactory: BankWithdrawCompletionFactory = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val onOrderCreated: (BankOfframpOrderResult) -> Unit = {}
    private val onDestinationChanged: (OfframpDestination) -> Unit = {}
    private val onNetworkFeeChanged: (BigDecimal) -> Unit = {}
    private val onCloseFlow: () -> Unit = mockk(relaxed = true)
    private var completedCount = 0
    private var outcomeUnknownCount = 0

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, completion, completionFactory, getUserWalletUseCase, analytics, onCloseFlow)
        completedCount = 0
        outcomeUnknownCount = 0
        every { getUserWalletUseCase(userWalletId) } returns mockk<UserWallet.Cold>().right()
        every { completionFactory.create(any(), any(), any(), any(), any()) } returns completion
        completionInFlight()
    }

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
        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) {
            completionFactory.create(sourceAccount, any(), onOrderCreated, onDestinationChanged, onNetworkFeeChanged)
        }
        model.onDestroy()
    }

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
    fun `GIVEN recipient not loaded WHEN model created THEN send disabled`() {
        // Act
        val model = createModel(draft = savedRecipientDraft(recipient = null))

        // Assert
        assertThat(summary(model).isSendEnabled).isFalse()
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

    @Test
    fun `GIVEN cold wallet WHEN model created THEN summary marks the nfc wallet`() {
        // Arrange
        every { getUserWalletUseCase(userWalletId) } returns mockk<UserWallet.Cold>().right()

        // Act
        val model = createModel()

        // Assert
        assertThat(summary(model).isColdWallet).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN hot wallet WHEN model created THEN summary marks no nfc wallet`() {
        // Arrange
        every { getUserWalletUseCase(userWalletId) } returns mockk<UserWallet.Hot>().right()

        // Act
        val model = createModel()

        // Assert
        assertThat(summary(model).isColdWallet).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet lookup fails WHEN model created THEN icon kept`() {
        // Arrange
        every { getUserWalletUseCase(userWalletId) } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        val model = createModel()

        // Assert
        assertThat(summary(model).isColdWallet).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN complete draft WHEN model created THEN ConfirmationScreenOpened is sent`() {
        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN new recipient WHEN recipient card tapped THEN RecipientDetailsOnConfirmationClicked is sent`() {
        // Arrange
        val model = createModel(draft = newRecipientDraft())

        // Act
        summary(model).recipientCards.first().onClick?.invoke()

        // Assert
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.Offramp.RecipientDetailsOnConfirmationClicked())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN new recipient WHEN bank card tapped THEN RecipientBankDetailsOnConfirmationClicked is sent`() {
        // Arrange
        val model = createModel(draft = newRecipientDraft())

        // Act
        summary(model).recipientCards.last().onClick?.invoke()

        // Assert
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsOnConfirmationClicked())
        }
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
            analytics.send(MoneyAccountAnalyticsEvents.SendFailed(errorCode = FORBIDDEN_ERROR_CODE))
        }
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents>()) }
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
    fun `GIVEN withdrawal in progress WHEN send THEN no SendErrorScreenOpened is sent`() {
        // Arrange
        completionReports(CompletionState.Sending, CompletionState.WithdrawalInProgress)
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        verify(exactly = 0) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN card prompt aborted WHEN completion idle again THEN no SendErrorScreenOpened is sent`() {
        // Arrange
        completionReports(CompletionState.Sending, CompletionState.Idle)
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

    private fun provideOutcomeUnknownModels() = listOf(
        OutcomeUnknownModel(failure = PreviewFailure.General, expectedRefreshRequests = 0),
        OutcomeUnknownModel(failure = PreviewFailure.TransferOutcomeUnknown, expectedRefreshRequests = 1),
    )

    internal data class OutcomeUnknownModel(val failure: PreviewFailure, val expectedRefreshRequests: Int)

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

    private fun createModel(
        draft: BankWithdrawDraft = savedRecipientDraft(),
        isBalanceHidden: Boolean = false,
        sourceAccount: BankWithdrawSourceAccount = this.sourceAccount,
    ): TangemPayBankWithdrawPreviewModel =
        TangemPayBankWithdrawPreviewModel(
            paramsContainer = MutableParamsContainer(
                TangemPayBankWithdrawPreviewComponent.Params(
                    sourceAccount = sourceAccount,
                    draft = MutableStateFlow(draft),
                    onOrderCreated = onOrderCreated,
                    onDestinationChanged = onDestinationChanged,
                    onNetworkFeeChanged = onNetworkFeeChanged,
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
            analytics = analytics,
            getBalanceHidingSettingsUseCase = mockk { every { isBalanceHidden() } returns flowOf(isBalanceHidden) },
        )

    private companion object {
        const val FORBIDDEN_ERROR_CODE = 104_120_100
        const val TRANSFER_CONFIRMED = "VA Offramp Recipient Transfer Confirmed"
        const val SEND_CONFIRMED = "VA Send Confirmed"
        const val SEND_FAILED = "VA Send failed"

        val MONEY_ACCOUNT = BankWithdrawSourceAccount.Money(UserWalletId("011"), productInstanceId = "instance-1")
        val MONEY_ACCOUNT_SEND_EVENTS = setOf(TRANSFER_CONFIRMED, SEND_CONFIRMED, SEND_FAILED)
    }
}