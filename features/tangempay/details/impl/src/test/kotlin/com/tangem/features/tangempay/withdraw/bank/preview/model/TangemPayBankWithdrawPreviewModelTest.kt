package com.tangem.features.tangempay.withdraw.bank.preview.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.usecase.bankofframp.AwaitBankOfframpSignatureUseCase
import com.tangem.domain.pay.usecase.bankofframp.CancelBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.SignBankOfframpOrderUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.personalFormValues
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Failed
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Summary
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawPreviewModelTest {

    private val userWalletId = UserWalletId("011")
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val createBankOfframpOrderUseCase: CreateBankOfframpOrderUseCase = mockk()
    private val awaitBankOfframpSignatureUseCase: AwaitBankOfframpSignatureUseCase = mockk()
    private val signBankOfframpOrderUseCase: SignBankOfframpOrderUseCase = mockk()
    private val cancelBankOfframpOrderUseCase: CancelBankOfframpOrderUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val created = mutableListOf<BankOfframpOrderResult>()
    private var signedCount = 0

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            createBankOfframpOrderUseCase,
            awaitBankOfframpSignatureUseCase,
            signBankOfframpOrderUseCase,
            cancelBankOfframpOrderUseCase,
            getUserWalletUseCase,
            analytics,
        )
        created.clear()
        signedCount = 0
        every { getUserWalletUseCase(userWalletId) } returns mockk<UserWallet.Cold>().right()
        coEvery { cancelBankOfframpOrderUseCase(userWalletId, any()) } returns Unit.right()
        // The order sits in processing unless a test says otherwise, which keeps the screen in its sending state.
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
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
    fun `GIVEN order created WHEN send THEN request sent once order reported and sending stays on`() {
        // Arrange
        val result = BankOfframpOrderResult(orderId = "order-1", bankAccountId = "acc-1")
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns result.right()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        val request = requireNotNull(completeDraft().toOrderRequest())
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, request, any()) }
        assertThat(created).containsExactly(result)
        assertThat(summary(model).isSending).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN request in flight WHEN send again THEN second request not sent`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel()

        // Act
        summary(model).onSendClick()
        summary(model).onSendClick()

        // Assert
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN unknown error WHEN send and retry THEN general failure and the idempotency key is reused`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.Unknown(null).left())
        val model = createModel()

        // Act
        summary(model).onSendClick()
        val failed = model.uiState.value as Failed
        requireNotNull(failed.onRetryClick).invoke()

        // Assert
        assertThat(failed.title).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_error_general_title))
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        assertThat(keys.toSet()).hasSize(1)
        assertThat(model.uiState.value).isInstanceOf(Failed::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN validation error WHEN send THEN general failure with retry`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns BankOfframpError.Validation.left()
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
    fun `GIVEN polling timeout error WHEN send THEN general failure with retry`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.PollingTimeout.left())
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        val failed = model.uiState.value as Failed
        assertThat(failed.title).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_error_general_title))
        assertThat(failed.onRetryClick).isNotNull()
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN unsupported error WHEN send THEN unavailable failure without retry`(model: UnavailableModel) {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns model.error.left()
        val previewModel = createModel()

        // Act
        summary(previewModel).onSendClick()

        // Assert
        val failed = previewModel.uiState.value as Failed
        assertThat(failed.title).isEqualTo(resourceReference(model.titleRes))
        assertThat(failed.onRetryClick).isNull()
        previewModel.onDestroy()
    }

    @Test
    fun `GIVEN withdrawal in progress WHEN send twice THEN sheet shown summary kept and a new key is used`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.WithdrawalInProgress.left())
        val model = createModel()

        // Act
        summary(model).onSendClick()
        val afterFirst = summary(model)
        summary(model).onSendClick()

        // Assert
        verify(exactly = 2) { uiMessageSender.send(any()) }
        assertThat(afterFirst.isSending).isFalse()
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        assertThat(keys.toSet()).hasSize(2)
        model.onDestroy()
    }

    @Test
    fun `GIVEN request in flight WHEN back or close THEN navigation ignored`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel()
        summary(model).onSendClick()

        // Act
        summary(model).onBackClick()
        summary(model).onCloseClick()

        // Assert
        verify(exactly = 0) { router.pop(any()) }
        verify(exactly = 0) { router.popTo(any<Route>(), any()) }
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
    fun `GIVEN summary WHEN close THEN wizard and hub popped to account details`() {
        // Arrange
        val model = createModel()

        // Act
        summary(model).onCloseClick()

        // Assert
        verify(exactly = 1) { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN failed WHEN close THEN wizard and hub popped to account details`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.Unknown(null).left())
        val model = createModel()
        summary(model).onSendClick()

        // Act
        (model.uiState.value as Failed).onCloseClick()

        // Assert
        verify(exactly = 1) { router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN recipient not loaded WHEN send THEN nothing sent`() {
        // Arrange
        val model = createModel(draft = completeDraft(recipient = null))

        // Act
        summary(model).onSendClick()

        // Assert
        coVerify(exactly = 0) { createBankOfframpOrderUseCase(any(), any(), any()) }
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
    fun `GIVEN request in flight WHEN edit requested THEN nothing pushed`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
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
    fun `GIVEN signature required WHEN payload signed THEN order reported as signed and sending ends`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Signed.right()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        assertThat(signedCount).isEqualTo(1)
        assertThat(summary(model).isSending).isFalse()
        coVerify(exactly = 1) { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN order created WHEN signature submitted THEN the same idempotency key used`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Signed.right()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        val keys = mutableListOf<String>()
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        coVerify(exactly = 1) {
            signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, capture(keys))
        }
        assertThat(keys).hasSize(2)
        assertThat(keys.toSet()).hasSize(1)
        model.onDestroy()
    }

    @Test
    fun `GIVEN card prompt aborted WHEN signing THEN order cancelled and summary editable again`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Cancelled.right()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        coVerify(exactly = 1) { cancelBankOfframpOrderUseCase(userWalletId, ORDER_ID) }
        assertThat(signedCount).isEqualTo(0)
        val state = summary(model)
        assertThat(state.isSending).isFalse()
        assertThat(state.isSendEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN polling timed out WHEN retry THEN the same order is resumed instead of a second one`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) }
            .returnsMany(listOf(BankOfframpError.PollingTimeout.left(), PAYLOAD_HASH.right()))
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Signed.right()
        val model = createModel()
        summary(model).onSendClick()

        // Act
        requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

        // Assert
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
        coVerify(exactly = 2) { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) }
        assertThat(signedCount).isEqualTo(1)
        model.onDestroy()
    }

    @Test
    fun `GIVEN order fails after creation WHEN not a timeout THEN the order is cancelled`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns
            BankOfframpError.Unknown(null).left()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        coVerify(exactly = 1) { cancelBankOfframpOrderUseCase(userWalletId, ORDER_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN polling times out WHEN failure shown THEN the order is kept for retry`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns
            BankOfframpError.PollingTimeout.left()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        coVerify(exactly = 0) { cancelBankOfframpOrderUseCase(any(), any()) }
        assertThat((model.uiState.value as Failed).onRetryClick).isNotNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the backend gave up on the order WHEN retry THEN a new order is created`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns
            BankOfframpError.Unknown(null).left()
        val model = createModel()
        summary(model).onSendClick()

        // Act
        requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

        // Assert
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
        model.onDestroy()
    }

    private fun orderCreated() {
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns
            BankOfframpOrderResult(orderId = ORDER_ID, bankAccountId = "acc-1").right()
    }

    private fun provideTestModels() = listOf(
        UnavailableModel(
            error = BankOfframpError.RecipientRegionUnsupported,
            titleRes = R.string.tangempay_withdraw_bank_error_recipient_region,
        ),
        UnavailableModel(
            error = BankOfframpError.SenderRegionUnsupported,
            titleRes = R.string.tangempay_withdraw_bank_error_sender_region,
        ),
        UnavailableModel(
            error = BankOfframpError.BusinessUnsupported,
            titleRes = R.string.tangempay_withdraw_bank_error_business,
        ),
        UnavailableModel(
            error = BankOfframpError.UnsupportedConfiguration,
            titleRes = R.string.tangempay_withdraw_bank_error_region_title,
        ),
    )

    internal data class UnavailableModel(val error: BankOfframpError, val titleRes: Int)

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
    fun `GIVEN request in flight WHEN edit requested THEN no offramp event is sent`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
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
    fun `GIVEN summary WHEN send tapped THEN SendOnConfirmationClicked is sent and the order is created`() {
        // Arrange
        orderCreated()
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()) }
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN request in flight WHEN send tapped again THEN SendOnConfirmationClicked is sent once`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel()

        // Act
        summary(model).onSendClick()
        summary(model).onSendClick()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN order creation fails WHEN send THEN SendErrorScreenOpened is sent`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.Unknown(null).left())
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN unsupported configuration WHEN send THEN SendErrorScreenOpened is sent`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.UnsupportedConfiguration.left())
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN polling times out WHEN awaiting the signature THEN SendErrorScreenOpened is sent`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns
            BankOfframpError.PollingTimeout.left()
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
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.Unknown(null).left())
        val model = createModel()
        summary(model).onSendClick()

        // Act
        requireNotNull((model.uiState.value as Failed).onRetryClick).invoke()

        // Assert
        verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()) }
        verify(exactly = 2) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN withdrawal in progress WHEN send THEN no SendErrorScreenOpened is sent`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.WithdrawalInProgress.left())
        val model = createModel()

        // Act
        summary(model).onSendClick()

        // Assert
        verify(exactly = 0) { analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN card prompt aborted WHEN signing THEN no SendErrorScreenOpened is sent`() {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Cancelled.right()
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
    fun `GIVEN request in flight WHEN rail row tapped THEN no RailSelectorClicked is sent`() {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } coAnswers { awaitCancellation() }
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

    private fun completeDraft(recipient: BankAccount? = bankAccount(id = "acc-1")) = BankWithdrawDraft(
        entry = BankWithdrawEntry.Saved("acc-1"),
        recipient = recipient,
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    private fun createModel(draft: BankWithdrawDraft = completeDraft()): TangemPayBankWithdrawPreviewModel =
        TangemPayBankWithdrawPreviewModel(
            paramsContainer = MutableParamsContainer(
                TangemPayBankWithdrawPreviewComponent.Params(
                    userWalletId = userWalletId,
                    draft = MutableStateFlow(draft),
                    onOrderCreated = created::add,
                    onOrderSigned = { signedCount++ },
                ),
            ),
            dispatchers = dispatchers,
            router = router,
            uiMessageSender = uiMessageSender,
            createBankOfframpOrderUseCase = createBankOfframpOrderUseCase,
            awaitBankOfframpSignatureUseCase = awaitBankOfframpSignatureUseCase,
            signBankOfframpOrderUseCase = signBankOfframpOrderUseCase,
            cancelBankOfframpOrderUseCase = cancelBankOfframpOrderUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            analytics = analytics,
        )

    private companion object {
        const val ORDER_ID = "order-1"
        const val PAYLOAD_HASH = "0xpayload"
    }
}