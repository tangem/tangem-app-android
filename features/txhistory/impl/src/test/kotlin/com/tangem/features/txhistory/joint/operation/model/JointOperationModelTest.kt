package com.tangem.features.txhistory.joint.operation.model

import android.text.format.DateFormat
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.signers.FinishedJointOperationSigners
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.features.txhistory.model.TxDetailsMenuActions
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationModelTest {

    private val delegate: JointOperationDelegate = mockk()
    private val poller: JointOperationPoller = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val menuActions: TxDetailsMenuActions = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()

    private val currency = MockCryptoCurrencyFactory().ethereum

    @BeforeAll
    fun mockDateFormat() {
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterAll
    fun unmockDateFormat() {
        unmockkStatic(DateFormat::class)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            delegate,
            poller,
            uiMessageSender,
            clipboardManager,
            analyticsEventHandler,
            menuActions,
            getBalanceHidingSettingsUseCase,
        )
        every { delegate.isColdWallet(userWalletId = any()) } returns false
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(JointOperationSnapshot.InFlight(createState()))
        coEvery { poller.poll(userWalletId = any(), operationId = any()) } coAnswers {
            awaitCancellation()
        }
        every { poller.refresh() } returns Unit
        every { menuActions.menu(txInfo = any(), currency = any(), onAddContact = any()) } returns persistentListOf()
        every { delegate.recipient(userWalletId = any(), recipient = any(), network = any()) } answers {
            flowOf(JointOperationRecipient(owner = ResolvedOwner.External(secondArg()), canCreateContact = false))
        }
        every { delegate.fiat(userWalletId = any(), currency = any()) } returns
            flowOf(JointOperationFiat(rate = null, appCurrency = AppCurrency.Default))
        every { getBalanceHidingSettingsUseCase() } returns flowOf(
            BalanceHidingSettings(
                isHidingEnabledInSettings = false,
                isBalanceHidden = false,
                isBalanceHidingNotificationEnabled = false,
            ),
        )
    }

    @Test
    fun `GIVEN the backend reports the threshold reached WHEN sign THEN Operation Signed counts the initiator`() =
        runTest {
            // Arrange
            val signedOperation = createOperation(
                status = JointOperation.Status.PENDING,
                signatures = listOf(signature(address = OWN)),
            )
            coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns
                signedOperation.right()
            val model = createModel()

            // Act
            model.onSignClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    event = JointAccountAnalyticsEvent.OperationSigned(
                        signaturesCollected = 2,
                        membersCount = 3,
                        threshold = 2,
                        isThresholdReached = true,
                        blockchain = currency.network.name,
                        token = currency.symbol,
                    ),
                )
            }
        }

    @Test
    fun `GIVEN the operation executed at once WHEN sign THEN the initiator counts once and threshold is reached`() =
        runTest {
            // Arrange
            val signedOperation = createOperation(
                status = JointOperation.Status.UNKNOWN,
                signatures = listOf(signature(address = INITIATOR), signature(address = OWN)),
            )
            coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns
                signedOperation.right()
            val model = createModel()

            // Act
            model.onSignClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    event = JointAccountAnalyticsEvent.OperationSigned(
                        signaturesCollected = 2,
                        membersCount = 3,
                        threshold = 2,
                        isThresholdReached = true,
                        blockchain = currency.network.name,
                        token = currency.symbol,
                    ),
                )
            }
        }

    @Test
    fun `GIVEN the sign response has no initiator WHEN sign THEN the initiator of the sheet is counted`() = runTest {
        // Arrange
        val signedOperation = createOperation(
            status = JointOperation.Status.PENDING,
            signatures = listOf(signature(address = OWN)),
        ).copy(initiator = null)
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns signedOperation.right()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = JointAccountAnalyticsEvent.OperationSigned(
                    signaturesCollected = 2,
                    membersCount = 3,
                    threshold = 2,
                    isThresholdReached = true,
                    blockchain = currency.network.name,
                    token = currency.symbol,
                ),
            )
        }
    }

    @Test
    fun `GIVEN signatures still below the threshold WHEN sign THEN threshold not reached`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(JointOperationSnapshot.InFlight(createState(threshold = 3)))
        val signedOperation = createOperation(
            status = JointOperation.Status.AWAITING_SIGNATURES,
            signatures = listOf(signature(address = OWN)),
        )
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns signedOperation.right()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = JointAccountAnalyticsEvent.OperationSigned(
                    signaturesCollected = 2,
                    membersCount = 3,
                    threshold = 3,
                    isThresholdReached = false,
                    blockchain = currency.network.name,
                    token = currency.symbol,
                ),
            )
        }
    }

    @Test
    fun `GIVEN the backend answer could not be read WHEN sign THEN no event is sent`() = runTest {
        // Arrange
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns null.right()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN signing is dismissed WHEN sign THEN no event is sent`() = runTest {
        // Arrange
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns
            JointOperationSignError.UserCancelled.left()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideSignErrorModels")
    fun `GIVEN signing fails with a shown error WHEN sign THEN Joint Account Error is sent`(model: SignErrorModel) =
        runTest {
            // Arrange
            coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns model.error.left()
            val sheet = createModel()

            // Act
            sheet.onSignClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(event = model.expected) }
        }

    data class SignErrorModel(val error: JointOperationSignError, val expected: JointAccountError)

    private fun provideSignErrorModels() = listOf(
        SignErrorModel(
            error = JointOperationSignError.Failed(cause = failure),
            expected = JointAccountError(source = ErrorSource.Sign, cause = failure),
        ),
        SignErrorModel(
            error = JointOperationSignError.ThresholdReached,
            expected = JointAccountError(source = ErrorSource.Sign, errorMessage = "ThresholdReached"),
        ),
    )

    @ParameterizedTest
    @MethodSource("provideRejectErrorModels")
    fun `GIVEN refusing fails WHEN refusal confirmed THEN Joint Account Error carries the refusal kind`(
        model: RejectErrorModel,
    ) = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(JointOperationSnapshot.InFlight(createState(ownAddress = model.ownAddress)))
        coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } returns
            JointOperationRejectError.Failed(cause = failure).left()
        val sheet = createModel()

        // Act
        sheet.confirmRefusal()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(event = JointAccountError(source = model.expectedSource, cause = failure))
        }
    }

    data class RejectErrorModel(val ownAddress: String, val expectedSource: ErrorSource)

    private fun provideRejectErrorModels() = listOf(
        RejectErrorModel(ownAddress = OWN, expectedSource = ErrorSource.Decline),
        RejectErrorModel(ownAddress = INITIATOR, expectedSource = ErrorSource.Delete),
    )

    @Test
    fun `GIVEN the operation is already gone WHEN refusal confirmed THEN no error is sent`() = runTest {
        // Arrange
        coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } returns
            JointOperationRejectError.OperationNotFound.left()
        val sheet = createModel()

        // Act
        sheet.confirmRefusal()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideRefusingMembers")
    fun `GIVEN this member refuses WHEN the threshold drops out of reach THEN the sheet stays`(ownAddress: String) =
        runTest {
            // Arrange
            every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
                flowOf(JointOperationSnapshot.InFlight(createState(ownAddress = ownAddress, threshold = 3)))
            coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } returns Unit.right()
            val sheet = createModel()

            // Act
            sheet.confirmRefusal()
            advanceUntilIdle()

            // Assert
            assertThat(sheet.uiState.value).isInstanceOf(JointOperationUM.Content::class.java)
        }

    private fun provideRefusingMembers() = listOf(OWN, INITIATOR)

    @Test
    fun `GIVEN another member makes the threshold unreachable WHEN the list arrives THEN it waits for the outcome`() =
        runTest {
            // Arrange
            every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
                flowOf(JointOperationSnapshot.InFlight(declinedByThird()))

            // Act
            val model = createModel()

            // Assert
            val content = model.uiState.value as JointOperationUM.Content
            assertThat(content.operation.cosignAction?.isLoading).isTrue()
            assertThat(content.operation.cosignAction?.isEnabled).isFalse()
        }

    @Test
    fun `GIVEN another member makes the threshold unreachable WHEN it comes back cancelled THEN the outcome shows`() =
        runTest {
            // Arrange
            val snapshots = MutableStateFlow<JointOperationSnapshot>(JointOperationSnapshot.InFlight(declinedByThird()))
            every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns snapshots
            val model = createModel()

            // Act
            snapshots.value = cancelledSnapshot
            advanceUntilIdle()

            // Assert
            val content = model.uiState.value as JointOperationUM.Content
            assertThat(content.operation.cosignAction).isNull()
        }

    @Test
    fun `GIVEN a finished operation WHEN the sheet shows it THEN the header carries the history menu`() = runTest {
        // Arrange
        val menu = persistentListOf(
            TxHistoryDetailsUM.MenuItemUM(
                icon = Icons.ic_globe_24,
                title = stringReference("Explore"),
                action = TxHistoryDetailsUM.MenuItemUM.Action.Direct {},
            ),
        )
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(finishedSnapshot)
        every { menuActions.menu(txInfo = finishedSnapshot.tx, currency = any(), onAddContact = null) } returns menu

        // Act
        val model = createModel()

        // Assert
        assertThat((model.uiState.value as JointOperationUM.Content).operation.header.menu)
            .containsAtLeastElementsIn(menu)
    }

    @Test
    fun `GIVEN a held operation WHEN the sheet shows it THEN it offers neither signing nor refusing`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(JointOperationSnapshot.InFlight(createState(), isHeld = true))
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as JointOperationUM.Content
        assertThat(content.operation.cosignAction?.isLoading).isTrue()
        assertThat(content.operation.cosignAction?.isEnabled).isFalse()
        assertThat(content.operation.header.menu.map { it.isDestructive }).doesNotContain(true)
        coVerify(exactly = 0) { delegate.sign(state = any(), network = any(), operationId = any()) }
    }

    private fun declinedByThird() = createState(threshold = 3).copy(
        signers = listOf(
            signer(address = INITIATOR, answer = Answer.CREATED),
            signer(address = OWN, answer = Answer.WAITING),
            signer(address = THIRD, answer = Answer.DECLINED),
        ),
    )

    @Test
    fun `GIVEN a refusal in progress WHEN the list drops the threshold out of reach THEN the sheet stays`() = runTest {
        // Arrange
        val pending = createState(threshold = 3)
        val snapshots = MutableStateFlow<JointOperationSnapshot>(JointOperationSnapshot.InFlight(pending))
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns snapshots
        val refusal = CompletableDeferred<Unit>()
        coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } coAnswers {
            snapshots.value = JointOperationSnapshot.InFlight(
                pending.copy(
                    signers = listOf(
                        signer(address = INITIATOR, answer = Answer.CREATED),
                        signer(address = OWN, answer = Answer.DECLINED),
                        signer(address = THIRD, answer = Answer.WAITING),
                    ),
                ),
            )
            refusal.await()
            Unit.right()
        }
        val sheet = createModel()

        // Act
        sheet.confirmRefusal()
        advanceUntilIdle()

        // Assert
        assertThat(sheet.uiState.value).isInstanceOf(JointOperationUM.Content::class.java)
        refusal.complete(Unit)
    }

    @Test
    fun `GIVEN the initiator deleted the operation WHEN the cancellation is not fetched yet THEN it keeps loading`() =
        runTest {
            // Arrange
            every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
                flowOf(JointOperationSnapshot.InFlight(createState(ownAddress = INITIATOR)))
            coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } returns Unit.right()
            val model = createModel()

            // Act
            model.confirmRefusal()
            advanceUntilIdle()

            // Assert
            val content = model.uiState.value as JointOperationUM.Content
            assertThat(content.operation.cosignAction?.isLoading).isTrue()
            assertThat(content.operation.cosignAction?.isEnabled).isFalse()
        }

    @Test
    fun `GIVEN the initiator deleted the operation WHEN it comes back cancelled THEN the sheet shows the outcome`() =
        runTest {
            // Arrange
            val snapshots = MutableStateFlow<JointOperationSnapshot>(
                JointOperationSnapshot.InFlight(createState(ownAddress = INITIATOR)),
            )
            every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns snapshots
            coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } returns Unit.right()
            val model = createModel()
            model.confirmRefusal()
            advanceUntilIdle()

            // Act
            snapshots.value = cancelledSnapshot
            advanceUntilIdle()

            // Assert
            val content = model.uiState.value as JointOperationUM.Content
            assertThat(content.operation.cosignAction).isNull()
        }

    @Test
    fun `GIVEN nothing stored yet WHEN the sheet opens THEN it is loading`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(JointOperationSnapshot.NotSynced)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isEqualTo(JointOperationUM.Loading)
    }

    @Test
    fun `GIVEN an unreadable operation WHEN the sheet opens THEN the sheet is gone`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(JointOperationSnapshot.Unreadable)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isEqualTo(JointOperationUM.Gone)
    }

    @Test
    fun `GIVEN a sending operation WHEN it finishes THEN the sheet shows the outcome with no actions`() = runTest {
        // Arrange
        val snapshots = MutableStateFlow<JointOperationSnapshot>(
            JointOperationSnapshot.InFlight(createState().copy(isSending = true)),
        )
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns snapshots
        val model = createModel()

        // Act
        snapshots.value = finishedSnapshot
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value as JointOperationUM.Content
        assertThat(content.operation.cosignAction).isNull()
        assertThat(content.operation.cosign?.progress).isEqualTo(SignatureProgressUM(collected = 2, total = 2))
        assertThat(content.signers).isEqualTo(finishedSnapshot.signatures.signers)
    }

    @Test
    fun `GIVEN the backend no longer knows the operation WHEN polled THEN the sheet is gone`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(heldSnapshot())
        coEvery { poller.poll(userWalletId = any(), operationId = any()) } returns
            JointOperationPoller.End.GONE

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isEqualTo(JointOperationUM.Gone)
    }

    @Test
    fun `GIVEN the sheet is not resumed WHEN created THEN the operation is not polled`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(heldSnapshot())

        // Act
        createModel(isResumed = false)

        // Assert
        coVerify(exactly = 0) { poller.poll(userWalletId = any(), operationId = any()) }
    }

    @Test
    fun `GIVEN polling runs WHEN the sheet is paused and resumed THEN polling stops and starts again`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(heldSnapshot())
        val model = createModel()

        // Act
        model.onPause()
        advanceUntilIdle()
        model.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { poller.poll(userWalletId = any(), operationId = any()) }
    }

    @Test
    fun `GIVEN polling ended WHEN the sheet is resumed again THEN the operation is not polled again`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(heldSnapshot())
        coEvery { poller.poll(userWalletId = any(), operationId = any()) } returns
            JointOperationPoller.End.FINISHED
        val model = createModel()

        // Act
        model.onPause()
        model.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { poller.poll(userWalletId = any(), operationId = any()) }
    }

    @Test
    fun `GIVEN the operation is in the list WHEN the sheet is resumed THEN it is not polled`() = runTest {
        // Act
        createModel()

        // Assert
        coVerify(exactly = 0) { poller.poll(userWalletId = any(), operationId = any()) }
    }

    @ParameterizedTest
    @MethodSource("provideAwaitedSnapshots")
    fun `GIVEN the list no longer carries the operation WHEN the sheet is resumed THEN it is polled`(
        awaited: JointOperationSnapshot,
    ) = runTest {
        // Arrange
        val snapshots = MutableStateFlow<JointOperationSnapshot>(JointOperationSnapshot.InFlight(createState()))
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns snapshots
        createModel()

        // Act
        snapshots.value = awaited
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { poller.poll(userWalletId = USER_WALLET_ID, operationId = OPERATION_ID) }
    }

    private fun provideAwaitedSnapshots() = listOf(JointOperationSnapshot.NotSynced, heldSnapshot())

    @Test
    fun `GIVEN the list carries the operation again WHEN polling runs THEN polling stops`() = runTest {
        // Arrange
        val snapshots = MutableStateFlow<JointOperationSnapshot>(heldSnapshot())
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns snapshots
        var isPolling = false
        coEvery { poller.poll(userWalletId = any(), operationId = any()) } coAnswers {
            isPolling = true
            try {
                awaitCancellation()
            } finally {
                isPolling = false
            }
        }
        createModel()

        // Act
        snapshots.value = JointOperationSnapshot.InFlight(createState())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { poller.poll(userWalletId = any(), operationId = any()) }
        assertThat(isPolling).isFalse()
    }

    private fun heldSnapshot() = JointOperationSnapshot.InFlight(createState(), isHeld = true)

    @Test
    fun `GIVEN the operation finished WHEN polling ends THEN the sheet stays`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(finishedSnapshot)
        coEvery { poller.poll(userWalletId = any(), operationId = any()) } returns
            JointOperationPoller.End.FINISHED

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointOperationUM.Content::class.java)
    }

    @Test
    fun `GIVEN a successful signature WHEN sign THEN the operation is fetched at once`() = runTest {
        // Arrange
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns null.right()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { poller.refresh() }
    }

    @Test
    fun `GIVEN a failed signature WHEN sign THEN nothing is fetched at once`() = runTest {
        // Arrange
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns
            JointOperationSignError.UserCancelled.left()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { poller.refresh() }
    }

    @ParameterizedTest
    @MethodSource("provideStaleSignErrors")
    fun `GIVEN the operation moved on WHEN signing fails THEN the sheet stays and fetches it at once`(
        error: JointOperationSignError,
    ) = runTest {
        // Arrange
        coEvery { delegate.sign(state = any(), network = any(), operationId = any()) } returns error.left()
        val model = createModel()

        // Act
        model.onSignClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointOperationUM.Content::class.java)
        verify(exactly = 1) { poller.refresh() }
    }

    private fun provideStaleSignErrors() = listOf(
        JointOperationSignError.OperationNotFound,
        JointOperationSignError.ThresholdReached,
    )

    @ParameterizedTest
    @MethodSource("provideStaleRejectErrors")
    fun `GIVEN the operation moved on WHEN refusing fails THEN the sheet stays and fetches it at once`(
        error: JointOperationRejectError,
    ) = runTest {
        // Arrange
        coEvery { delegate.reject(state = any(), network = any(), operationId = any()) } returns error.left()
        val model = createModel()

        // Act
        model.confirmRefusal()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointOperationUM.Content::class.java)
        verify(exactly = 1) { poller.refresh() }
    }

    private fun provideStaleRejectErrors() = listOf(
        JointOperationRejectError.OperationNotFound,
        JointOperationRejectError.ThresholdReached,
    )

    /** The refusal runs from the confirmation dialog the sheet shows first */
    private fun JointOperationModel.confirmRefusal() {
        val dialogs = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(dialogs)) } returns Unit
        onRefuseClick()
        (dialogs.single() as DialogMessage).firstAction.onClick()
    }

    /** The actions read the state the sheet shows, so the test collects it the way the sheet does */
    private fun TestScope.createModel(isResumed: Boolean = true): JointOperationModel {
        val model = JointOperationModel(
            paramsContainer = MutableParamsContainer(
                JointOperationComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    currency = currency,
                    operationId = OPERATION_ID,
                    onDismiss = {},
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            delegate = delegate,
            poller = poller,
            uiMessageSender = uiMessageSender,
            clipboardManager = clipboardManager,
            analyticsEventHandler = analyticsEventHandler,
            menuActions = menuActions,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        )
        if (isResumed) model.onResume()
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()

        return model
    }

    private fun createState(ownAddress: String = OWN, threshold: Int = 2) = JointOperationState(
        account = MockAccounts.createJointAccount(),
        displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = BigInteger.ONE),
        feeAmount = BigInteger.ZERO,
        feeCurrency = null,
        threshold = threshold,
        membersCount = 3,
        signers = listOf(
            signer(address = INITIATOR, answer = Answer.CREATED),
            signer(address = OWN, answer = Answer.WAITING),
            signer(address = THIRD, answer = Answer.WAITING),
        ),
        canSign = true,
        ownAddress = ownAddress,
        initiator = INITIATOR,
    )

    private fun signer(address: String, answer: Answer) = JointOperationSignerState(
        member = JointAccount.Member(name = "Isaac", address = address, role = JointAccount.Role.MEMBER),
        answer = answer,
        at = null,
    )

    private fun createOperation(status: JointOperation.Status, signatures: List<JointOperation.MemberSignature>) =
        JointOperation(
            operationId = OPERATION_ID,
            chainId = 1L,
            safeTx = SafeTx(
                to = RECIPIENT,
                value = BigInteger.ONE,
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger.ZERO,
                baseGas = BigInteger.ZERO,
                gasPrice = BigInteger.ZERO,
                gasToken = ZERO_ADDRESS,
                refundReceiver = ZERO_ADDRESS,
                nonce = BigInteger.ZERO,
            ),
            status = status,
            initiator = INITIATOR,
            signatures = signatures,
        )

    private fun signature(address: String) = JointOperation.MemberSignature(
        address = address,
        state = JointOperation.MemberSignature.State.SIGNED,
        at = null,
    )

    private val finishedSnapshot: JointOperationSnapshot.Finished
        get() = JointOperationSnapshot.Finished(
            tx = JointOperationTx(
                operation = FinishedJointOperation(
                    operationId = OPERATION_ID,
                    chainId = 1L,
                    safeAddress = SAFE_ADDRESS,
                    safeTx = createOperation(status = JointOperation.Status.PENDING, signatures = emptyList()).safeTx,
                    status = FinishedJointOperation.Status.EXECUTED,
                    initiator = INITIATOR,
                    signatures = listOf(signature(address = OWN)),
                    createdAt = "2026-10-14T09:20:37.000Z",
                    finishedAtMillis = 1_000L,
                    txHash = "0xhash",
                ),
                transfer = JointOperationTransfer(
                    currency = currency,
                    intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = BigInteger.ONE),
                ),
                txInfo = null,
            ),
            signatures = FinishedJointOperationSigners.Signatures(
                collected = 2,
                threshold = 2,
                signers = JointOperationSignersUM(membersCount = 3, signers = persistentListOf()),
            ),
        )

    private val cancelledSnapshot: JointOperationSnapshot.Finished
        get() = finishedSnapshot.copy(
            tx = finishedSnapshot.tx.copy(
                operation = finishedSnapshot.tx.operation.copy(
                    status = FinishedJointOperation.Status.CANCELLED,
                    txHash = null,
                ),
            ),
        )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val SAFE_ADDRESS = "0xsafe"
        const val OPERATION_ID = "operation-1"
        const val INITIATOR = "0xinitiator"
        const val OWN = "0xowner"
        const val THIRD = "0xthird"
        const val RECIPIENT = "0xrecipient"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

        val failure = IllegalStateException("Request failed")
    }
}