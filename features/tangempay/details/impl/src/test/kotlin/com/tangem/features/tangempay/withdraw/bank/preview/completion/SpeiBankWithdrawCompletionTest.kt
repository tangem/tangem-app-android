package com.tangem.features.tangempay.withdraw.bank.preview.completion

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankAccountDetails
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpTransfer
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.Beneficiary
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.bankOfframpQuote
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransferError
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.speiDestination
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.time.CurrentTimeProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpeiBankWithdrawCompletionTest {

    private val userWalletId = UserWalletId("011")
    private val bankOfframpOrderRepository: BankOfframpOrderRepository = mockk()
    private val moneyAccountTransfer: MoneyAccountTransfer = mockk()
    private val draft = savedRecipientDraft().copy(destination = SPEI_DESTINATION, networkFee = NETWORK_FEE)
    private val token = requireNotNull(draft.source).token
    private val changedNetworkFees = mutableListOf<BigDecimal>()
    private val idempotencyKeys = mutableListOf<String>()
    private val states = mutableListOf<CompletionState>()
    private var now: DateTime = NOW

    @BeforeEach
    fun resetMocks() {
        clearMocks(bankOfframpOrderRepository, moneyAccountTransfer)
        now = NOW
        changedNetworkFees.clear()
        idempotencyKeys.clear()
        states.clear()
        coEvery {
            bankOfframpOrderRepository.createQuote(
                userWalletId = userWalletId,
                productInstanceId = PRODUCT_INSTANCE_ID,
                amount = any(),
                recipient = any(),
                idempotencyKey = capture(idempotencyKeys),
            )
        } returns quote().right()
        coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns transfer().right()
        coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN draft WHEN prepare THEN the amount is quoted for the recipient and the quote is held`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.prepare(draft, states::add)

        // Assert
        coVerify(exactly = 1) {
            bankOfframpOrderRepository.createQuote(
                userWalletId = userWalletId,
                productInstanceId = PRODUCT_INSTANCE_ID,
                amount = BigDecimal("500.00"),
                recipient = BankOfframpRecipient.Saved(bankAccountId = "acc-1"),
                idempotencyKey = any(),
            )
        }
        assertThat(completion.quote.value).isEqualTo(quote())
        assertThat(states).containsExactly(CompletionState.Idle)
    }

    @Test
    fun `GIVEN quote held WHEN prepare again for the same draft THEN no new quote is requested`() = runTest {
        // Arrange
        val completion = createCompletion()
        completion.prepare(draft, states::add)

        // Act
        completion.prepare(draft, states::add)

        // Assert
        coVerify(exactly = 1) { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) }
        assertThat(completion.quote.value).isEqualTo(quote())
    }

    @Test
    fun `GIVEN quote requested WHEN the answer is pending THEN the previous quote is no longer held`() = runTest {
        // Arrange
        val completion = createCompletion()
        completion.prepare(draft, states::add)
        val heldWhileRequested = mutableListOf<BankOfframpQuote?>()
        coEvery { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) } coAnswers {
            heldWhileRequested.add(completion.quote.value)
            quote(quoteId = "quote-2").right()
        }

        // Act
        completion.prepare(draft.copy(amount = BigDecimal("600")), states::add)

        // Assert
        assertThat(heldWhileRequested).containsExactly(null)
        assertThat(completion.quote.value).isEqualTo(quote(quoteId = "quote-2"))
    }

    @Test
    fun `GIVEN quote held WHEN complete THEN transfer created from the quote and its source amount sent`() = runTest {
        // Arrange
        val completion = createCompletion()
        completion.prepare(draft, states::add)
        states.clear()

        // Act
        completion.complete(draft, states::add)

        // Assert
        coVerify(exactly = 1) { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) }
        coVerify(exactly = 1) {
            moneyAccountTransfer.send(userWalletId, token, BigDecimal("500.00"), TRANSFER_ADDRESS, NETWORK_FEE)
        }
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Completed).inOrder()
    }

    @ParameterizedTest
    @MethodSource("provideMismatchModels")
    fun `GIVEN transfer or quote off the confirmed amount WHEN complete THEN general failure and nothing sent`(
        model: MismatchModel,
    ) = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) } returns
            quote(sourceAmount = model.quoteSourceAmount).right()
        coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns
            transfer(sourceAmount = model.transferSourceAmount).right()
        val completion = createCompletion()
        completion.prepare(draft, states::add)
        states.clear()

        // Act
        completion.complete(draft, states::add)

        // Assert
        coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
        assertThat(states).containsExactly(
            CompletionState.Sending,
            CompletionState.Failed(PreviewFailure.General),
        ).inOrder()
    }

    private fun provideMismatchModels() = listOf(
        MismatchModel(quoteSourceAmount = BigDecimal("500.00"), transferSourceAmount = BigDecimal("505.50")),
        MismatchModel(quoteSourceAmount = BigDecimal("505.50"), transferSourceAmount = BigDecimal("505.50")),
    )

    data class MismatchModel(val quoteSourceAmount: BigDecimal, val transferSourceAmount: BigDecimal)

    @Test
    fun `GIVEN no quote held WHEN complete THEN a quote is requested and nothing is sent`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)

        // Assert
        assertThat(completion.quote.value).isEqualTo(quote())
        coVerify(exactly = 0) { bankOfframpOrderRepository.createTransfer(any(), any()) }
        coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
        assertThat(states).containsExactly(CompletionState.Idle)
    }

    @Test
    fun `GIVEN quote within thirty seconds of expiry WHEN complete THEN a new quote under a new key and no transfer`() =
        runTest {
            // Arrange
            val expiring = quote(expiresAt = NOW.plusMinutes(2))
            coEvery {
                bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
            } returnsMany listOf(expiring.right(), quote().right())
            val completion = createCompletion()
            completion.prepare(draft, states::add)
            states.clear()
            now = expiring.expiresAt.minusSeconds(20)

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(completion.quote.value).isEqualTo(quote())
            assertThat(idempotencyKeys.distinct()).hasSize(2)
            coVerify(exactly = 0) { bankOfframpOrderRepository.createTransfer(any(), any()) }
            assertThat(states).containsExactly(CompletionState.Idle)
        }

    @ParameterizedTest
    @MethodSource("provideLateQuoteExpiries")
    fun `GIVEN quote late on arrival WHEN prepare and complete THEN no requote and the backend decides`(
        expiresAt: DateTime,
    ) =
        runTest {
            // Arrange
            coEvery {
                bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
            } returns quote(expiresAt = expiresAt).right()
            val completion = createCompletion()
            completion.prepare(draft, states::add)
            states.clear()

            // Act
            completion.prepare(draft, states::add)
            completion.complete(draft, states::add)

            // Assert
            assertThat(idempotencyKeys).hasSize(1)
            coVerify(exactly = 1) { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) }
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Completed).inOrder()
        }

    private fun provideLateQuoteExpiries() = listOf(NOW.minusMinutes(5), NOW.plusSeconds(60))

    @Test
    fun `GIVEN transfer amount equal to the quote in another scale WHEN complete THEN it is sent`() = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns
            transfer(sourceAmount = BigDecimal("500")).right()
        val completion = createCompletion()
        completion.prepare(draft, states::add)
        states.clear()

        // Act
        completion.complete(draft, states::add)

        // Assert
        coVerify(exactly = 1) {
            moneyAccountTransfer.send(userWalletId, token, BigDecimal("500"), TRANSFER_ADDRESS, NETWORK_FEE)
        }
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Completed).inOrder()
    }

    @Test
    fun `GIVEN quote held WHEN the clock passes its send deadline THEN prepare requests a new quote`() = runTest {
        // Arrange
        val completion = createCompletion()
        completion.prepare(draft, states::add)
        now = FAR_EXPIRY.minusSeconds(30)

        // Act
        completion.prepare(draft, states::add)

        // Assert
        assertThat(idempotencyKeys).hasSize(2)
        assertThat(idempotencyKeys.distinct()).hasSize(2)
    }

    @Test
    fun `GIVEN transfer refused as expired WHEN complete THEN a new quote under a new key and nothing sent`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns
                BankOfframpError.QuoteExpired.left()
            val completion = createCompletion()
            completion.prepare(draft, states::add)
            states.clear()

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(idempotencyKeys).hasSize(2)
            assertThat(idempotencyKeys.distinct()).hasSize(2)
            coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
        }

    @ParameterizedTest
    @MethodSource("provideTransientFailureModels")
    fun `GIVEN unanswered quote requests WHEN prepare THEN terms keep loading and are asked again under the same key`(
        model: TransientFailureModel,
    ) = runTest {
        // Arrange
        val answers: List<Either<BankOfframpError, BankOfframpQuote>> =
            List(model.failures) { model.error.left() } + quote().right()
        coEvery {
            bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
        } returnsMany answers
        val completion = createCompletion()
        val heldWhileRetrying = mutableListOf<BankOfframpQuote?>()
        launch { completion.prepare(draft, states::add) }
        runCurrent()
        heldWhileRetrying.add(completion.quote.value)

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(heldWhileRetrying).containsExactly(null)
        assertThat(states).containsExactly(CompletionState.Idle)
        assertThat(completion.quote.value).isEqualTo(quote())
        assertThat(testScheduler.currentTime).isEqualTo(model.expectedElapsedMillis)
        assertThat(idempotencyKeys).hasSize(model.failures + 1)
        assertThat(idempotencyKeys.distinct()).hasSize(1)
    }

    @Test
    fun `GIVEN quote requests unanswered for two minutes WHEN prepare THEN general failure and no more requests`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) } returns
                OFFLINE.left()
            val completion = createCompletion()

            // Act
            completion.prepare(draft, states::add)

            // Assert
            assertThat(states).containsExactly(
                CompletionState.Idle,
                CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104110999),
            ).inOrder()
            assertThat(testScheduler.currentTime).isEqualTo(120_000L)
            coVerify(exactly = 8) { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN new recipient quoted once WHEN quoted again THEN the backend id of the recipient is used`() = runTest {
        // Arrange
        val newRecipientDraft = newSpeiRecipientDraft()
        coEvery {
            bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
        } returnsMany listOf(
            quote(bankAccountId = "acc-new", expiresAt = NOW.plusMinutes(2)).right(),
            quote(bankAccountId = "acc-new").right(),
        )
        val completion = createCompletion()
        completion.prepare(newRecipientDraft, states::add)
        now = NOW.plusMinutes(2).minusSeconds(10)

        // Act
        completion.complete(newRecipientDraft, states::add)

        // Assert
        coVerify(exactly = 1) {
            bankOfframpOrderRepository.createQuote(
                userWalletId = userWalletId,
                productInstanceId = PRODUCT_INSTANCE_ID,
                amount = BigDecimal("500.00"),
                recipient = BankOfframpRecipient.New(details = SPEI_RECIPIENT_DETAILS),
                idempotencyKey = any(),
            )
        }
        coVerify(exactly = 1) {
            bankOfframpOrderRepository.createQuote(
                userWalletId = userWalletId,
                productInstanceId = PRODUCT_INSTANCE_ID,
                amount = BigDecimal("500.00"),
                recipient = BankOfframpRecipient.Saved(bankAccountId = "acc-new"),
                idempotencyKey = any(),
            )
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN quote refused WHEN prepare THEN the failure follows the error and no quote is held`(
        model: RefusalModel,
    ) = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) } returns
            model.error.left()
        val completion = createCompletion()

        // Act
        completion.prepare(draft, states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Idle, model.expected).inOrder()
        assertThat(completion.quote.value).isNull()
        coVerify(exactly = 0) { bankOfframpOrderRepository.createTransfer(any(), any()) }
    }

    @Test
    fun `GIVEN transfer rejected WHEN complete THEN general failure with the code and the next send quotes again`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns
                BankOfframpError.Rejected.left()
            val completion = createCompletion()
            completion.prepare(draft, states::add)
            states.clear()

            // Act
            completion.complete(draft, states::add)
            completion.complete(draft, states::add)

            // Assert
            assertThat(states).containsExactly(
                CompletionState.Sending,
                CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104140169),
                CompletionState.Idle,
            ).inOrder()
            assertThat(idempotencyKeys).hasSize(2)
        }

    @ParameterizedTest
    @MethodSource("provideSendFailureModels")
    fun `GIVEN send fails WHEN complete THEN the state follows the failure`(model: SendFailureModel) = runTest {
        // Arrange
        coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns model.error.left()
        val completion = createCompletion()
        completion.prepare(draft, states::add)
        states.clear()

        // Act
        completion.complete(draft, states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Sending, model.expected).inOrder()
        assertThat(changedNetworkFees).isEqualTo(model.expectedNetworkFees)
    }

    @Test
    fun `GIVEN send outcome unknown WHEN complete again THEN nothing is requested and the outcome stays unknown`() =
        runTest {
            // Arrange
            coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns
                MoneyAccountTransferError.OutcomeUnknown.left()
            val completion = createCompletion()
            completion.prepare(draft, states::add)
            completion.complete(draft, states::add)
            states.clear()

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(states).containsExactly(CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown))
            coVerify(exactly = 1) { bankOfframpOrderRepository.createTransfer(any(), any()) }
        }

    private fun provideTestModels() = listOf(
        RefusalModel(
            error = BankOfframpError.Validation,
            expected = CompletionState.Failed(failure = PreviewFailure.General),
        ),
        RefusalModel(
            error = BankOfframpError.SenderRegionUnsupported,
            expected = CompletionState.Failed(failure = PreviewFailure.SenderRegion, errorCode = 104140165),
        ),
        RefusalModel(
            error = BankOfframpError.AmountOutOfLimits,
            expected = CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104140168),
        ),
        RefusalModel(
            error = BankOfframpError.VerificationIncomplete,
            expected = CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104140166),
        ),
        RefusalModel(
            error = BankOfframpError.Unknown(cause = null, errorCode = 104110001),
            expected = CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104110001),
        ),
        RefusalModel(
            error = BankOfframpError.QuoteExpired,
            expected = CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104140164),
        ),
        RefusalModel(
            error = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true, errorCode = 104004001),
            expected = CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104004001),
        ),
        RefusalModel(
            error = BankOfframpError.Unknown(cause = null),
            expected = CompletionState.Failed(failure = PreviewFailure.General),
        ),
    )

    data class RefusalModel(val error: BankOfframpError, val expected: CompletionState)

    private fun provideTransientFailureModels() = listOf(
        TransientFailureModel(error = OFFLINE, failures = 1, expectedElapsedMillis = 2_000L),
        TransientFailureModel(error = OFFLINE, failures = 3, expectedElapsedMillis = 14_000L),
        TransientFailureModel(error = OFFLINE, failures = 6, expectedElapsedMillis = 90_000L),
        TransientFailureModel(error = SERVER_UNAVAILABLE, failures = 1, expectedElapsedMillis = 2_000L),
    )

    data class TransientFailureModel(
        val error: BankOfframpError,
        val failures: Int,
        val expectedElapsedMillis: Long,
    )

    private fun provideSendFailureModels() = listOf(
        SendFailureModel(error = MoneyAccountTransferError.Cancelled, expected = CompletionState.Idle),
        SendFailureModel(
            error = MoneyAccountTransferError.NotSent,
            expected = CompletionState.Failed(PreviewFailure.General),
        ),
        SendFailureModel(
            error = MoneyAccountTransferError.OutcomeUnknown,
            expected = CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown),
        ),
        SendFailureModel(
            error = MoneyAccountTransferError.NetworkFeeIncreased(networkFee = BigDecimal("0.07")),
            expected = CompletionState.Idle,
            expectedNetworkFees = listOf(BigDecimal("0.07")),
        ),
    )

    data class SendFailureModel(
        val error: MoneyAccountTransferError,
        val expected: CompletionState,
        val expectedNetworkFees: List<BigDecimal> = emptyList(),
    )

    private fun createCompletion() = SpeiBankWithdrawCompletion(
        userWalletId = userWalletId,
        productInstanceId = PRODUCT_INSTANCE_ID,
        onNetworkFeeChanged = changedNetworkFees::add,
        bankOfframpOrderRepository = bankOfframpOrderRepository,
        moneyAccountTransfer = moneyAccountTransfer,
        currentTimeProvider = CurrentTimeProvider { now.millis },
    )

    private fun newSpeiRecipientDraft() = BankWithdrawDraft(
        entry = BankWithdrawEntry.NewPersonal,
        newRecipient = NewRecipientDraft(
            fields = mapOf(
                BankWithdrawFormField.Clabe to SPEI_RECIPIENT_DETAILS.accountNumber,
                BankWithdrawFormField.FirstName to "Maria",
                BankWithdrawFormField.LastName to "Lopez",
            ),
        ),
        amount = BigDecimal("500"),
        source = draft.source,
        destination = SPEI_DESTINATION,
        networkFee = NETWORK_FEE,
    )

    private fun quote(
        quoteId: String = QUOTE_ID,
        bankAccountId: String = "acc-1",
        expiresAt: DateTime = FAR_EXPIRY,
        sourceAmount: BigDecimal = BigDecimal("500.00"),
    ) =
        bankOfframpQuote(
            quoteId = quoteId,
            bankAccountId = bankAccountId,
            sourceAmount = sourceAmount,
            destinationAmount = BigDecimal("8530.13"),
            exchangeRate = BigDecimal("17.25"),
            expiresAt = expiresAt,
        )

    private fun transfer(sourceAmount: BigDecimal = BigDecimal("500.00")) = BankOfframpTransfer(
        transferId = "transfer-1",
        depositAddress = TRANSFER_ADDRESS,
        sourceAmount = sourceAmount,
        destinationAmount = BigDecimal("8530.13"),
        expiresAt = NOW.plusMinutes(30),
    )

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi-1"
        const val QUOTE_ID = "quote-1"
        const val TRANSFER_ADDRESS = "0x0000000000000000000000000000000000000004"
        val NETWORK_FEE = BigDecimal("0.03")
        val NOW: DateTime = DateTime.parse("2026-11-10T10:00:00Z")
        val FAR_EXPIRY: DateTime = NOW.plusMinutes(15)
        val OFFLINE = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true, errorCode = 104110999)
        val SERVER_UNAVAILABLE = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true, errorCode = 104004006)
        val SPEI_DESTINATION = speiDestination()
        val SPEI_RECIPIENT_DETAILS = BankAccountDetails(
            currency = "MXN",
            rail = BankRail.SPEI,
            isThirdParty = false,
            beneficiary = Beneficiary.Individual(firstName = "Maria", lastName = "Lopez"),
            beneficiaryAddress = null,
            bankName = null,
            bankAddress = null,
            accountNumber = "002010077777777771",
            routingNumber = null,
            accountType = null,
        )
    }
}