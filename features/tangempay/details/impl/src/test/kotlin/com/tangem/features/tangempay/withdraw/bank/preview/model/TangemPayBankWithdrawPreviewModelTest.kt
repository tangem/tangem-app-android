package com.tangem.features.tangempay.withdraw.bank.preview.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Failed
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.Summary
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val created = mutableListOf<BankOfframpOrderResult>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, createBankOfframpOrderUseCase)
        created.clear()
    }

    @Test
    fun `GIVEN complete draft WHEN model created THEN summary with transfer recipient cards and send enabled`() {
        // Act
        val model = createModel()

        // Assert
        val state = summary(model)
        assertThat(state.header.title).isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_confirm_title))
        assertThat(state.transfer?.amount?.amount).isEqualTo("$500")
        assertThat(state.recipientCards.map { it.rows.size }).containsExactly(2, 7, 6).inOrder()
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
    )

    internal data class UnavailableModel(val error: BankOfframpError, val titleRes: Int)

    private fun summary(model: TangemPayBankWithdrawPreviewModel): Summary = model.uiState.value as Summary

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
                ),
            ),
            dispatchers = dispatchers,
            router = router,
            uiMessageSender = uiMessageSender,
            createBankOfframpOrderUseCase = createBankOfframpOrderUseCase,
        )
}