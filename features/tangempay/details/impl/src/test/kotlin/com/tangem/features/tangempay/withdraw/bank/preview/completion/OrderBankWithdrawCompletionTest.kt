package com.tangem.features.tangempay.withdraw.bank.preview.completion

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.usecase.bankofframp.AwaitBankOfframpSignatureUseCase
import com.tangem.domain.pay.usecase.bankofframp.CancelBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.SignBankOfframpOrderUseCase
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.toOrderRequest
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderBankWithdrawCompletionTest {

    private val userWalletId = UserWalletId("011")
    private val createBankOfframpOrderUseCase: CreateBankOfframpOrderUseCase = mockk()
    private val awaitBankOfframpSignatureUseCase: AwaitBankOfframpSignatureUseCase = mockk()
    private val signBankOfframpOrderUseCase: SignBankOfframpOrderUseCase = mockk()
    private val cancelBankOfframpOrderUseCase: CancelBankOfframpOrderUseCase = mockk()
    private val request = requireNotNull(savedRecipientDraft().toOrderRequest())
    private val created = mutableListOf<BankOfframpOrderResult>()
    private val states = mutableListOf<CompletionState>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            createBankOfframpOrderUseCase,
            awaitBankOfframpSignatureUseCase,
            signBankOfframpOrderUseCase,
            cancelBankOfframpOrderUseCase,
        )
        created.clear()
        states.clear()
        coEvery { cancelBankOfframpOrderUseCase(userWalletId, any()) } returns Unit.right()
        // The order sits in processing unless a test says otherwise, which keeps the attempt in flight.
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, any(), any(), any()) } coAnswers {
            awaitCancellation()
        }
    }

    @Test
    fun `GIVEN order created WHEN complete THEN request sent once order reported and sending stays on`() = runTest {
        // Arrange
        val result = BankOfframpOrderResult(orderId = ORDER_ID, bankAccountId = "acc-1")
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns result.right()
        val completion = createCompletion()

        // Act
        backgroundScope.launch { completion.complete(savedRecipientDraft(), states::add) }
        runCurrent()

        // Assert
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, request, any()) }
        assertThat(created).containsExactly(result)
        assertThat(states).containsExactly(CompletionState.Sending)
    }

    @Test
    fun `GIVEN recipient not loaded WHEN complete THEN nothing sent and no state reported`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(recipient = null), states::add)

        // Assert
        coVerify(exactly = 0) { createBankOfframpOrderUseCase(any(), any(), any()) }
        assertThat(states).isEmpty()
    }

    @Test
    fun `GIVEN outcome unknown WHEN complete twice THEN general failure and the idempotency key is reused`() = runTest {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true).left())
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        assertThat(keys.toSet()).hasSize(1)
        assertThat(states).containsExactly(
            CompletionState.Sending,
            CompletionState.Failed(PreviewFailure.General),
            CompletionState.Sending,
            CompletionState.Failed(PreviewFailure.General),
        ).inOrder()
    }

    @Test
    fun `GIVEN a definitive error WHEN complete twice THEN a new idempotency key is used`() = runTest {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.Unknown(cause = null, isOutcomeUnknown = false).left())
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        assertThat(keys.toSet()).hasSize(2)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN order creation error WHEN complete THEN mapped failure reported`(model: FailureModel) = runTest {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns model.error.left()
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Failed(model.failure)).inOrder()
    }

    @Test
    fun `GIVEN withdrawal in progress WHEN complete twice THEN reported each time and a new key is used`() = runTest {
        // Arrange
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            .returns(BankOfframpError.WithdrawalInProgress.left())
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        assertThat(states).containsExactly(
            CompletionState.Sending,
            CompletionState.WithdrawalInProgress,
            CompletionState.Sending,
            CompletionState.WithdrawalInProgress,
        ).inOrder()
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        assertThat(keys.toSet()).hasSize(2)
    }

    @Test
    fun `GIVEN signature required WHEN payload signed THEN completed reported`() = runTest {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Signed.right()
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Completed).inOrder()
        coVerify(exactly = 1) { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) }
    }

    @Test
    fun `GIVEN order created WHEN awaiting signature THEN the payload is checked against the draft order`() = runTest {
        // Arrange
        orderCreated()
        val completion = createCompletion()

        // Act
        backgroundScope.launch { completion.complete(savedRecipientDraft(), states::add) }
        runCurrent()

        // Assert
        coVerify(exactly = 1) { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, request, any()) }
    }

    @Test
    fun `GIVEN order created WHEN signature submitted THEN the same idempotency key used`() = runTest {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Signed.right()
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        val keys = mutableListOf<String>()
        coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, any(), capture(keys)) }
        coVerify(exactly = 1) {
            signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, capture(keys))
        }
        assertThat(keys).hasSize(2)
        assertThat(keys.toSet()).hasSize(1)
    }

    @Test
    fun `GIVEN card prompt aborted WHEN signing THEN order cancelled and idle reported`() = runTest {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) } returns PAYLOAD_HASH.right()
        coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
            BankOfframpSignResult.Cancelled.right()
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        coVerify(exactly = 1) { cancelBankOfframpOrderUseCase(userWalletId, ORDER_ID) }
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
    }

    @Test
    fun `GIVEN polling timed out WHEN complete again THEN the same order is resumed instead of a second one`() =
        runTest {
            // Arrange
            orderCreated()
            coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) }
                .returnsMany(listOf(BankOfframpError.PollingTimeout.left(), PAYLOAD_HASH.right()))
            coEvery { signBankOfframpOrderUseCase(userWalletId, ORDER_ID, PAYLOAD_HASH, any()) } returns
                BankOfframpSignResult.Signed.right()
            val completion = createCompletion()
            completion.complete(savedRecipientDraft(), states::add)

            // Act
            completion.complete(savedRecipientDraft(), states::add)

            // Assert
            coVerify(exactly = 1) { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
            coVerify(exactly = 2) { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, request, any()) }
            assertThat(states).containsExactly(
                CompletionState.Sending,
                CompletionState.Failed(PreviewFailure.General),
                CompletionState.Sending,
                CompletionState.Completed,
            ).inOrder()
        }

    @Test
    fun `GIVEN order fails after creation WHEN not a timeout THEN the order is cancelled`() = runTest {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) } returns
            BankOfframpError.Unknown(null).left()
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { cancelBankOfframpOrderUseCase(userWalletId, ORDER_ID) }
        assertThat(states.last()).isEqualTo(CompletionState.Failed(PreviewFailure.General))
    }

    @Test
    fun `GIVEN polling times out WHEN failure reported THEN the order is kept for retry`() = runTest {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) } returns
            BankOfframpError.PollingTimeout.left()
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(), states::add)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { cancelBankOfframpOrderUseCase(any(), any()) }
        assertThat(states.last()).isEqualTo(CompletionState.Failed(PreviewFailure.General))
    }

    @Test
    fun `GIVEN the backend gave up on the order WHEN complete again THEN a new order is created`() = runTest {
        // Arrange
        orderCreated()
        coEvery { awaitBankOfframpSignatureUseCase(userWalletId, ORDER_ID, any(), any()) } returns
            BankOfframpError.Unknown(null).left()
        val completion = createCompletion()
        completion.complete(savedRecipientDraft(), states::add)

        // Act
        completion.complete(savedRecipientDraft(), states::add)

        // Assert
        coVerify(exactly = 2) { createBankOfframpOrderUseCase(userWalletId, any(), any()) }
    }

    private fun orderCreated() {
        coEvery { createBankOfframpOrderUseCase(userWalletId, any(), any()) } returns
            BankOfframpOrderResult(orderId = ORDER_ID, bankAccountId = "acc-1").right()
    }

    private fun TestScope.createCompletion() = OrderBankWithdrawCompletion(
        userWalletId = userWalletId,
        scope = backgroundScope,
        onOrderCreated = created::add,
        createBankOfframpOrderUseCase = createBankOfframpOrderUseCase,
        awaitBankOfframpSignatureUseCase = awaitBankOfframpSignatureUseCase,
        signBankOfframpOrderUseCase = signBankOfframpOrderUseCase,
        cancelBankOfframpOrderUseCase = cancelBankOfframpOrderUseCase,
    )

    private fun provideTestModels() = listOf(
        FailureModel(error = BankOfframpError.Validation, failure = PreviewFailure.General),
        FailureModel(error = BankOfframpError.PollingTimeout, failure = PreviewFailure.General),
        FailureModel(error = BankOfframpError.Unknown(cause = null), failure = PreviewFailure.General),
        FailureModel(error = BankOfframpError.RecipientRegionUnsupported, failure = PreviewFailure.RecipientRegion),
        FailureModel(error = BankOfframpError.SenderRegionUnsupported, failure = PreviewFailure.SenderRegion),
        FailureModel(error = BankOfframpError.BusinessUnsupported, failure = PreviewFailure.Business),
        FailureModel(
            error = BankOfframpError.UnsupportedConfiguration,
            failure = PreviewFailure.UnsupportedConfiguration,
        ),
    )

    internal data class FailureModel(val error: BankOfframpError, val failure: PreviewFailure)

    private companion object {
        const val ORDER_ID = "order-1"
        const val PAYLOAD_HASH = "0xpayload"
    }
}