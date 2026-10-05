package com.tangem.features.txhistory.joint.operation.model

import arrow.core.left
import arrow.core.right
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

/** Covers the `Operation Signed` analytics sent once the backend accepts this member's signature */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationModelTest {

    private val delegate: JointOperationDelegate = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()

    private val currency = MockCryptoCurrencyFactory().ethereum

    @BeforeEach
    fun resetMocks() {
        clearMocks(delegate, uiMessageSender, clipboardManager, analyticsEventHandler, getBalanceHidingSettingsUseCase)
        every { delegate.isColdWallet(userWalletId = any()) } returns false
        every { delegate.observe(userWalletId = any(), currency = any(), operationId = any()) } returns
            flowOf(createState())
        every { delegate.canCreateContact(userWalletId = any(), recipient = any(), network = any()) } returns
            flowOf(false)
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
    fun `GIVEN the initiator is among the votes WHEN sign THEN it is counted once`() = runTest {
        // Arrange
        val signedOperation = createOperation(
            status = JointOperation.Status.AWAITING_SIGNATURES,
            signatures = listOf(signature(address = INITIATOR), signature(address = OWN)),
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
                    threshold = 2,
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

    /** The actions read the state the sheet shows, so the test collects it the way the sheet does */
    private fun TestScope.createModel(): JointOperationModel {
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
            uiMessageSender = uiMessageSender,
            clipboardManager = clipboardManager,
            analyticsEventHandler = analyticsEventHandler,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        )
        backgroundScope.launch { model.uiState.collect {} }
        advanceUntilIdle()

        return model
    }

    private fun createState() = JointOperationState(
        account = MockAccounts.createJointAccount(),
        displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = BigInteger.ONE),
        feeWei = BigInteger.ZERO,
        feeCoin = null,
        threshold = 2,
        membersCount = 3,
        signers = listOf(
            signer(address = INITIATOR, answer = Answer.CREATED),
            signer(address = OWN, answer = Answer.WAITING),
            signer(address = THIRD, answer = Answer.WAITING),
        ),
        canSign = true,
        ownAddress = OWN,
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

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OPERATION_ID = "operation-1"
        const val INITIATOR = "0xinitiator"
        const val OWN = "0xowner"
        const val THIRD = "0xthird"
        const val RECIPIENT = "0xrecipient"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}