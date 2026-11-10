package com.tangem.features.tangempay.withdraw.bank.preview.completion

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.domain.tangempay.model.bankofframp.BankAccountDetails
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpTransfer
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.Beneficiary
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransferError
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private val quotes = mutableListOf<BankOfframpQuote>()
    private val changedNetworkFees = mutableListOf<BigDecimal>()
    private val idempotencyKeys = mutableListOf<String>()
    private val states = mutableListOf<CompletionState>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(bankOfframpOrderRepository, moneyAccountTransfer)
        quotes.clear()
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
    fun `GIVEN draft WHEN prepare THEN the amount is quoted for the recipient and the terms are published`() =
        runTest {
            // Arrange
            val completion = createCompletion()

            // Act
            completion.prepare(draft)

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
            assertThat(quotes).containsExactly(quote())
        }

    @Test
    fun `GIVEN terms shown WHEN complete THEN transfer created from the quote and its source amount sent`() = runTest {
        // Arrange
        val completion = createCompletion()
        completion.prepare(draft)

        // Act
        completion.complete(draft, states::add)

        // Assert
        coVerify(exactly = 1) { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) }
        coVerify(exactly = 1) {
            moneyAccountTransfer.send(userWalletId, token, BigDecimal("500.00"), TRANSFER_ADDRESS, NETWORK_FEE)
        }
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Completed).inOrder()
    }

    @Test
    fun `GIVEN terms not shown yet WHEN complete THEN the quote is published and nothing is sent`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)

        // Assert
        assertThat(quotes).containsExactly(quote())
        coVerify(exactly = 0) { bankOfframpOrderRepository.createTransfer(any(), any()) }
        coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
    }

    @Test
    fun `GIVEN quote within thirty seconds of expiry WHEN complete THEN a new quote under a new key and no transfer`() =
        runTest {
            // Arrange
            val expiring = quote(expiresAt = DateTime.now().plusSeconds(20))
            coEvery {
                bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
            } returnsMany listOf(expiring.right(), quote().right())
            val completion = createCompletion()
            completion.prepare(draft)

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(quotes).containsExactly(expiring, quote()).inOrder()
            assertThat(idempotencyKeys.distinct()).hasSize(2)
            coVerify(exactly = 0) { bankOfframpOrderRepository.createTransfer(any(), any()) }
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
        }

    @Test
    fun `GIVEN transfer refused as expired WHEN complete THEN a new quote under a new key and nothing sent`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns
                BankOfframpError.QuoteExpired.left()
            val completion = createCompletion()
            completion.prepare(draft)

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(quotes).hasSize(2)
            assertThat(idempotencyKeys.distinct()).hasSize(2)
            coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
        }

    @Test
    fun `GIVEN quote request without an answer WHEN retried THEN the same idempotency key is resent`() = runTest {
        // Arrange
        coEvery {
            bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
        } returnsMany listOf(OUTCOME_UNKNOWN.left(), quote().right())
        val completion = createCompletion()
        completion.prepare(draft)

        // Act
        completion.complete(draft, states::add)

        // Assert
        assertThat(idempotencyKeys).hasSize(2)
        assertThat(idempotencyKeys.distinct()).hasSize(1)
        assertThat(quotes).containsExactly(quote())
    }

    @Test
    fun `GIVEN new recipient quoted once WHEN quoted again THEN the backend id of the recipient is used`() = runTest {
        // Arrange
        val newRecipientDraft = newSpeiRecipientDraft()
        coEvery {
            bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), capture(idempotencyKeys))
        } returnsMany listOf(
            quote(bankAccountId = "acc-new", expiresAt = DateTime.now().plusSeconds(10)).right(),
            quote(bankAccountId = "acc-new").right(),
        )
        val completion = createCompletion()
        completion.prepare(newRecipientDraft)

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
    fun `GIVEN quote refused WHEN complete THEN the state follows the error`(model: RefusalModel) = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createQuote(userWalletId, any(), any(), any(), any()) } returns
            model.error.left()
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Sending, model.expected).inOrder()
        coVerify(exactly = 0) { bankOfframpOrderRepository.createTransfer(any(), any()) }
    }

    @Test
    fun `GIVEN transfer rejected WHEN complete THEN general failure with the code and the next send quotes again`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createTransfer(userWalletId, QUOTE_ID) } returns
                BankOfframpError.Rejected.left()
            val completion = createCompletion()
            completion.prepare(draft)

            // Act
            completion.complete(draft, states::add)
            completion.complete(draft, states::add)

            // Assert
            assertThat(states).containsExactly(
                CompletionState.Sending,
                CompletionState.Failed(failure = PreviewFailure.General, errorCode = 104140169),
                CompletionState.Sending,
                CompletionState.Idle,
            ).inOrder()
            assertThat(quotes).hasSize(2)
        }

    @ParameterizedTest
    @MethodSource("provideSendFailureModels")
    fun `GIVEN send fails WHEN complete THEN the state follows the failure`(model: SendFailureModel) = runTest {
        // Arrange
        coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns model.error.left()
        val completion = createCompletion()
        completion.prepare(draft)

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
            completion.prepare(draft)
            completion.complete(draft, states::add)
            states.clear()

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(states).containsExactly(CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown))
            coVerify(exactly = 1) { bankOfframpOrderRepository.createTransfer(any(), any()) }
        }

    private fun provideTestModels() = listOf(
        RefusalModel(error = BankOfframpError.Validation, expected = CompletionState.Idle),
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
    )

    data class RefusalModel(val error: BankOfframpError, val expected: CompletionState)

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
        onQuoteChanged = quotes::add,
        onNetworkFeeChanged = changedNetworkFees::add,
        bankOfframpOrderRepository = bankOfframpOrderRepository,
        moneyAccountTransfer = moneyAccountTransfer,
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
        bankAccountId: String = "acc-1",
        expiresAt: DateTime = FAR_EXPIRY,
    ) = BankOfframpQuote(
        quoteId = QUOTE_ID,
        bankAccountId = bankAccountId,
        feeAmount = BigDecimal("5.50"),
        sourceAmount = BigDecimal("500.00"),
        destinationAmount = BigDecimal("8530.13"),
        destinationCurrency = "MXN",
        exchangeRate = BigDecimal("17.25"),
        fee = SPEI_DESTINATION.fee,
        eta = SPEI_DESTINATION.eta,
        expiresAt = expiresAt,
    )

    private fun transfer() = BankOfframpTransfer(
        transferId = "transfer-1",
        depositAddress = TRANSFER_ADDRESS,
        sourceAmount = BigDecimal("500.00"),
        destinationAmount = BigDecimal("8530.13"),
        expiresAt = DateTime.now().plusMinutes(30),
    )

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi-1"
        const val QUOTE_ID = "quote-1"
        const val TRANSFER_ADDRESS = "0x0000000000000000000000000000000000000004"
        val NETWORK_FEE = BigDecimal("0.03")
        val FAR_EXPIRY: DateTime = DateTime.parse("2099-01-01T00:00:00Z")
        val OUTCOME_UNKNOWN = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true)
        val SPEI_DESTINATION = offrampDestination(
            rail = BankRail.SPEI,
            fee = BigDecimal("1"),
            calculationType = FeeCalculationType.PERCENTAGE,
            fixedFeeAmount = BigDecimal("0.50"),
            minDays = 0,
            maxDays = 0,
        )
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