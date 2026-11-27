package com.tangem.features.tangempay.withdraw.bank.preview.completion

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.domain.tangempay.model.bankofframp.BankAccountType
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRoute
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.domain.tangempay.model.bankofframp.OfframpEta
import com.tangem.domain.tangempay.model.bankofframp.OfframpFee
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransferError
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.model.toNewBankAccountDetails
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.personalFormValues
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RouteBankWithdrawCompletionTest {

    private val userWalletId = UserWalletId("011")
    private val bankOfframpOrderRepository: BankOfframpOrderRepository = mockk()
    private val moneyAccountTransfer: MoneyAccountTransfer = mockk()
    private val draft = savedRecipientDraft().copy(networkFee = NETWORK_FEE)
    private val token = requireNotNull(draft.source).token
    private val changedDestinations = mutableListOf<OfframpDestination>()
    private val changedNetworkFees = mutableListOf<BigDecimal>()
    private val states = mutableListOf<CompletionState>()
    private val newRecipientListener: NewRecipientListener = mockk(relaxed = true)
    private val newRecipientDraft = draft.copy(
        entry = BankWithdrawEntry.NewPersonal,
        recipient = null,
        newRecipient = NewRecipientDraft(
            isThirdParty = false,
            accountType = BankAccountType.CHECKING,
            fields = personalFormValues,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(bankOfframpOrderRepository, moneyAccountTransfer, newRecipientListener)
        changedDestinations.clear()
        changedNetworkFees.clear()
        states.clear()
        coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
            route().right()
        coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN recipient not loaded WHEN complete THEN nothing requested and no state reported`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.complete(savedRecipientDraft(recipient = null), states::add)

        // Assert
        coVerify(exactly = 0) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
        assertThat(states).isEmpty()
    }

    @Test
    fun `GIVEN route priced as shown WHEN complete THEN amount plus rail fee sent capped by the shown network fee`() =
        runTest {
            // Arrange
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)

            // Assert
            coVerify(exactly = 1) {
                bankOfframpOrderRepository.createRoute(
                    userWalletId = userWalletId,
                    productInstanceId = PRODUCT_INSTANCE_ID,
                    rail = BankRail.ACH,
                    recipient = BankOfframpRecipient.Saved(bankAccountId = "acc-1"),
                    idempotencyKey = any(),
                )
            }
            coVerify(exactly = 1) {
                moneyAccountTransfer.send(userWalletId, token, BigDecimal("501.00"), DEPOSIT_ADDRESS, NETWORK_FEE)
            }
            assertThat(changedDestinations).isEmpty()
            assertThat(changedNetworkFees).isEmpty()
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Completed).inOrder()
        }

    @Test
    fun `GIVEN route priced differently WHEN complete THEN draft gets the route fee and eta and nothing is sent`() =
        runTest {
            // Arrange
            val repriced = route(fee = BigDecimal("1.50"), minDays = 2, maxDays = 4)
            coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
                repriced.right()
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(changedDestinations).containsExactly(
                requireNotNull(draft.destination).copy(fee = repriced.fee, eta = repriced.eta),
            )
            coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
        }

    @Test
    fun `GIVEN route with a percentage fee WHEN complete twice THEN general failure, nothing sent, same route kept`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
                route(calculationType = FeeCalculationType.PERCENTAGE).right()
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)
            completion.complete(draft, states::add)

            // Assert
            coVerify(exactly = 1) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(changedDestinations).isEmpty()
            assertThat(states).containsExactly(
                CompletionState.Sending,
                CompletionState.Failed(PreviewFailure.General),
                CompletionState.Sending,
                CompletionState.Failed(PreviewFailure.General),
            ).inOrder()
        }

    @Test
    fun `GIVEN repriced route WHEN complete with the updated draft THEN same route used and new total sent`() =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
                route(fee = BigDecimal("1.50")).right()
            val completion = createCompletion()
            completion.complete(draft, states::add)

            // Act
            completion.complete(draft.copy(destination = changedDestinations.single()), states::add)

            // Assert
            coVerify(exactly = 1) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                moneyAccountTransfer.send(userWalletId, token, BigDecimal("501.50"), DEPOSIT_ADDRESS, NETWORK_FEE)
            }
            assertThat(states.last()).isEqualTo(CompletionState.Completed)
        }

    @Test
    fun `GIVEN network fee above the shown one WHEN complete THEN draft gets the new fee and idle reported`() =
        runTest {
            // Arrange
            coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns
                MoneyAccountTransferError.NetworkFeeIncreased(RAISED_NETWORK_FEE).left()
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)

            // Assert
            assertThat(changedNetworkFees).containsExactly(RAISED_NETWORK_FEE)
            assertThat(changedDestinations).isEmpty()
            assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
        }

    @Test
    fun `GIVEN network fee rose WHEN complete with the updated draft THEN same route used and new fee is the cap`() =
        runTest {
            // Arrange
            coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) }.returnsMany(
                listOf(MoneyAccountTransferError.NetworkFeeIncreased(RAISED_NETWORK_FEE).left(), Unit.right()),
            )
            val completion = createCompletion()
            completion.complete(draft, states::add)

            // Act
            completion.complete(draft.copy(networkFee = changedNetworkFees.single()), states::add)

            // Assert
            coVerify(exactly = 1) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                moneyAccountTransfer.send(
                    userWalletId = userWalletId,
                    token = token,
                    amount = BigDecimal("501.00"),
                    destination = DEPOSIT_ADDRESS,
                    maxNetworkFee = RAISED_NETWORK_FEE,
                )
            }
            assertThat(states.last()).isEqualTo(CompletionState.Completed)
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN route error WHEN complete THEN mapped state reported and nothing sent`(model: RouteErrorModel) =
        runTest {
            // Arrange
            coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
                model.error.left()
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)

            // Assert
            coVerify(exactly = 0) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(states).containsExactly(CompletionState.Sending, model.state).inOrder()
        }

    @ParameterizedTest
    @MethodSource("provideKeyReuseModels")
    fun `GIVEN route error WHEN complete twice THEN the idempotency key reused only for an unknown outcome`(
        model: KeyReuseModel,
    ) = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
            BankOfframpError.Unknown(cause = null, isOutcomeUnknown = model.isOutcomeUnknown).left()
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)
        completion.complete(draft, states::add)

        // Assert
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), capture(keys)) }
        assertThat(keys.toSet()).hasSize(model.expectedDistinctKeys)
    }

    @Test
    fun `GIVEN transfer not sent WHEN complete again THEN the same route is reused and the transfer retried`() =
        runTest {
            // Arrange
            coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) }
                .returnsMany(listOf(MoneyAccountTransferError.NotSent.left(), Unit.right()))
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)
            completion.complete(draft, states::add)

            // Assert
            coVerify(exactly = 1) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
            coVerify(exactly = 2) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(states).containsExactly(
                CompletionState.Sending,
                CompletionState.Failed(PreviewFailure.General),
                CompletionState.Sending,
                CompletionState.Completed,
            ).inOrder()
        }

    @ParameterizedTest
    @MethodSource("provideChangedAttemptModels")
    fun `GIVEN rail or recipient changed after a failed transfer WHEN complete THEN a new route with a new key`(
        model: ChangedAttemptModel,
    ) = runTest {
        // Arrange
        coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns
            MoneyAccountTransferError.NotSent.left()
        val completion = createCompletion()
        completion.complete(draft, states::add)

        // Act
        completion.complete(model.changedDraft, states::add)

        // Assert
        val rails = mutableListOf<BankRail>()
        val recipients = mutableListOf<BankOfframpRecipient>()
        val keys = mutableListOf<String>()
        coVerify(exactly = 2) {
            bankOfframpOrderRepository.createRoute(any(), any(), capture(rails), capture(recipients), capture(keys))
        }
        assertThat(rails).containsExactly(BankRail.ACH, model.expectedRail).inOrder()
        assertThat(recipients).containsExactly(SAVED_RECIPIENT, model.expectedRecipient).inOrder()
        assertThat(keys.toSet()).hasSize(2)
    }

    @Test
    fun `GIVEN transfer outcome unknown WHEN complete again THEN no second transfer and the failure stays`() =
        runTest {
            // Arrange
            coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns
                MoneyAccountTransferError.OutcomeUnknown.left()
            val completion = createCompletion()

            // Act
            completion.complete(draft, states::add)
            completion.complete(draft, states::add)

            // Assert
            coVerify(exactly = 1) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { moneyAccountTransfer.send(any(), any(), any(), any(), any()) }
            assertThat(states).containsExactly(
                CompletionState.Sending,
                CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown),
                CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown),
            ).inOrder()
        }

    @Test
    fun `GIVEN signature cancelled WHEN complete THEN idle reported`() = runTest {
        // Arrange
        coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returns
            MoneyAccountTransferError.Cancelled.left()
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)

        // Assert
        assertThat(states).containsExactly(CompletionState.Sending, CompletionState.Idle).inOrder()
    }

    @Test
    fun `GIVEN new recipient WHEN route created THEN the recipient reported as saved`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.complete(newRecipientDraft, states::add)

        // Assert
        verify(exactly = 1) { newRecipientListener.onSaved(requireNotNull(newRecipientDraft.toNewBankAccountDetails())) }
        verify(exactly = 0) { newRecipientListener.onRejected(any(), any()) }
    }

    @Test
    fun `GIVEN new recipient route reused after a failed transfer WHEN complete again THEN saved reported once`() =
        runTest {
            // Arrange
            coEvery { moneyAccountTransfer.send(userWalletId, any(), any(), any(), any()) } returnsMany listOf(
                MoneyAccountTransferError.NotSent.left(),
                Unit.right(),
            )
            val completion = createCompletion()
            completion.complete(newRecipientDraft, states::add)

            // Act
            completion.complete(newRecipientDraft, states::add)

            // Assert
            coVerify(exactly = 1) { bankOfframpOrderRepository.createRoute(any(), any(), any(), any(), any()) }
            verify(exactly = 1) { newRecipientListener.onSaved(any()) }
        }

    @Test
    fun `GIVEN saved recipient WHEN route created THEN no recipient result reported`() = runTest {
        // Arrange
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)

        // Assert
        verify(exactly = 0) { newRecipientListener.onSaved(any()) }
        verify(exactly = 0) { newRecipientListener.onRejected(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("provideRecipientRefusalModels")
    fun `GIVEN new recipient WHEN route request fails THEN only an answered refusal reported with its code`(
        model: RecipientRefusalModel,
    ) = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
            model.error.left()
        val completion = createCompletion()

        val wireDraft = newRecipientDraft.copy(destination = offrampDestination(rail = BankRail.WIRE))

        // Act
        completion.complete(wireDraft, states::add)

        // Assert
        verify(exactly = model.expectedRejections) {
            newRecipientListener.onRejected(
                recipient = requireNotNull(wireDraft.toNewBankAccountDetails()),
                errorCode = model.error.errorCode,
            )
        }
        verify(exactly = 0) { newRecipientListener.onSaved(any()) }
    }

    @Test
    fun `GIVEN saved recipient WHEN route refused THEN no recipient result reported`() = runTest {
        // Arrange
        coEvery { bankOfframpOrderRepository.createRoute(userWalletId, any(), any(), any(), any()) } returns
            BankOfframpError.Rejected.left()
        val completion = createCompletion()

        // Act
        completion.complete(draft, states::add)

        // Assert
        verify(exactly = 0) { newRecipientListener.onRejected(any(), any()) }
    }

    private fun createCompletion() = RouteBankWithdrawCompletion(
        userWalletId = userWalletId,
        productInstanceId = PRODUCT_INSTANCE_ID,
        onDestinationChanged = changedDestinations::add,
        onNetworkFeeChanged = changedNetworkFees::add,
        newRecipientListener = newRecipientListener,
        bankOfframpOrderRepository = bankOfframpOrderRepository,
        moneyAccountTransfer = moneyAccountTransfer,
    )

    private fun provideRecipientRefusalModels() = listOf(
        RecipientRefusalModel(error = BankOfframpError.BadRequest, expectedRejections = 1),
        RecipientRefusalModel(
            error = BankOfframpError.Unknown(cause = null, errorCode = BAD_REQUEST_ERROR_CODE),
            expectedRejections = 0,
        ),
        RecipientRefusalModel(error = BankOfframpError.RecipientRegionUnsupported, expectedRejections = 1),
        RecipientRefusalModel(error = BankOfframpError.BusinessUnsupported, expectedRejections = 1),
        RecipientRefusalModel(error = BankOfframpError.Validation, expectedRejections = 0),
        RecipientRefusalModel(error = BankOfframpError.Rejected, expectedRejections = 0),
        RecipientRefusalModel(error = BankOfframpError.AmountOutOfLimits, expectedRejections = 0),
        RecipientRefusalModel(
            error = BankOfframpError.Unknown(cause = null, errorCode = FORBIDDEN_ERROR_CODE),
            expectedRejections = 0,
        ),
        RecipientRefusalModel(
            error = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true),
            expectedRejections = 0,
        ),
        RecipientRefusalModel(
            error = BankOfframpError.Unknown(cause = null, errorCode = SERVER_UNAVAILABLE_ERROR_CODE),
            expectedRejections = 0,
        ),
        RecipientRefusalModel(error = BankOfframpError.PollingTimeout, expectedRejections = 0),
        RecipientRefusalModel(
            error = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true, errorCode = BAD_REQUEST_ERROR_CODE),
            expectedRejections = 0,
        ),
    )

    internal data class RecipientRefusalModel(val error: BankOfframpError, val expectedRejections: Int)

    private fun route(
        fee: BigDecimal = BigDecimal("1.00"),
        calculationType: FeeCalculationType = FeeCalculationType.FIXED,
        minDays: Int = 1,
        maxDays: Int = 3,
    ) = BankOfframpRoute(
        depositAddress = DEPOSIT_ADDRESS,
        fee = OfframpFee(
            name = null,
            calculationType = calculationType,
            amount = fee,
            currency = "USD",
            description = null,
        ),
        eta = OfframpEta(minDays = minDays, maxDays = maxDays),
        bankAccountId = "acc-1",
    )

    private fun provideTestModels() = listOf(
        RouteErrorModel(
            error = BankOfframpError.UnsupportedConfiguration,
            state = CompletionState.Failed(
                failure = PreviewFailure.UnsupportedConfiguration,
                errorCode = BankOfframpError.UnsupportedConfiguration.errorCode,
            ),
        ),
        RouteErrorModel(
            error = BankOfframpError.Unavailable(errorCode = FORBIDDEN_ERROR_CODE),
            state = CompletionState.Failed(
                failure = PreviewFailure.UnsupportedConfiguration,
                errorCode = FORBIDDEN_ERROR_CODE,
            ),
        ),
        RouteErrorModel(
            error = BankOfframpError.Unknown(cause = null, errorCode = BAD_REQUEST_ERROR_CODE),
            state = CompletionState.Failed(failure = PreviewFailure.General, errorCode = BAD_REQUEST_ERROR_CODE),
        ),
        RouteErrorModel(
            error = BankOfframpError.Unknown(cause = null, isOutcomeUnknown = true),
            state = CompletionState.Failed(PreviewFailure.General),
        ),
        RouteErrorModel(error = BankOfframpError.Validation, state = CompletionState.Idle),
    )

    internal data class RouteErrorModel(val error: BankOfframpError, val state: CompletionState)

    private fun provideKeyReuseModels() = listOf(
        KeyReuseModel(isOutcomeUnknown = true, expectedDistinctKeys = 1),
        KeyReuseModel(isOutcomeUnknown = false, expectedDistinctKeys = 2),
    )

    internal data class KeyReuseModel(val isOutcomeUnknown: Boolean, val expectedDistinctKeys: Int)

    private fun provideChangedAttemptModels() = listOf(
        ChangedAttemptModel(
            changedDraft = draft.copy(destination = offrampDestination(rail = BankRail.WIRE)),
            expectedRail = BankRail.WIRE,
            expectedRecipient = SAVED_RECIPIENT,
        ),
        ChangedAttemptModel(
            changedDraft = savedRecipientDraft(recipient = bankAccount(id = "acc-2")),
            expectedRail = BankRail.ACH,
            expectedRecipient = BankOfframpRecipient.Saved(bankAccountId = "acc-2"),
        ),
    )

    internal data class ChangedAttemptModel(
        val changedDraft: BankWithdrawDraft,
        val expectedRail: BankRail,
        val expectedRecipient: BankOfframpRecipient,
    )

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi-1"
        const val DEPOSIT_ADDRESS = "0x0000000000000000000000000000000000000003"
        val SAVED_RECIPIENT = BankOfframpRecipient.Saved(bankAccountId = "acc-1")
        val NETWORK_FEE = BigDecimal("0.03")
        val RAISED_NETWORK_FEE = BigDecimal("0.05")
        const val FORBIDDEN_ERROR_CODE = 104_120_100
        const val BAD_REQUEST_ERROR_CODE = 104_130_100
        val SERVER_UNAVAILABLE_ERROR_CODE = TangemPayApiError.ServerUnavailable.errorCode
    }
}