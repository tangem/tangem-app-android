package com.tangem.domain.jointaccount.verifier

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.gas.JointGasTokenResolver
import com.tangem.domain.jointaccount.gas.JointRefundReceiverProvider
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.math.BigInteger

internal class JointOperationDisplayGateTest {

    private val verifier: JointOperationVerifier = mockk()
    private val compositionVerifier: JointAccountCompositionVerifier = mockk()
    private val refundReceiverProvider: JointRefundReceiverProvider = mockk()
    private val gasTokenResolver: JointGasTokenResolver = mockk()
    private val network: Network = mockk {
        every { id } returns Network.ID(value = "polygon", derivationPath = Network.DerivationPath.None)
    }
    private val currency: CryptoCurrency = mockk()

    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 0)
    private val transfer = JointOperationTransfer(currency = currency, intent = DISPLAYED)

    private val gate = JointOperationDisplayGate(
        verifier = verifier,
        compositionVerifier = compositionVerifier,
        refundReceiverProvider = refundReceiverProvider,
        gasTokenResolver = gasTokenResolver,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(verifier, compositionVerifier, refundReceiverProvider, gasTokenResolver)
        coEvery { compositionVerifier.ownerAddress(userWalletId = any(), ownerKeyIndex = any()) } returns ALICE
        coEvery { refundReceiverProvider.refundReceiver() } returns REFUND_RECEIVER
        coEvery { gasTokenResolver.findUnpriced(network = any(), address = any()) } returns
            SafeGasToken.Coin.right()
        every { verifier.verify(any(), any(), any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN the operation verifies WHEN isShown THEN it is shown without the gas price ceiling`() = runTest {
        // Act
        val actual = isShown()

        // Assert
        assertThat(actual).isTrue()
        verify(exactly = 1) {
            verifier.verify(
                jointAccount = JOINT_ACCOUNT,
                operation = OPERATION,
                displayed = DISPLAYED,
                ownerAddress = ALICE,
                refundBounds = JointOperationVerifier.RefundBounds(
                    refundReceiver = REFUND_RECEIVER,
                    networkGasPrice = null,
                    gasToken = SafeGasToken.Coin,
                ),
            )
        }
    }

    @Test
    fun `GIVEN the operation does not verify WHEN isShown THEN it is hidden`() = runTest {
        // Arrange
        every { verifier.verify(any(), any(), any(), any(), any()) } returns
            VerificationFailed(reason = VerificationFailed.Reason.VOTE_SIGNATURE_MISMATCH).left()

        // Act
        val actual = isShown()

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN no owner address WHEN isShown THEN it is hidden before the verifier runs`() = runTest {
        // Arrange
        coEvery { compositionVerifier.ownerAddress(userWalletId = any(), ownerKeyIndex = any()) } returns null

        // Act
        val actual = isShown()

        // Assert
        assertThat(actual).isFalse()
        verify(exactly = 0) { verifier.verify(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the same operation twice WHEN isShown THEN the verdict is reused`() = runTest {
        // Act
        val first = isShown()
        val second = isShown()

        // Assert
        assertThat(listOf(first, second)).containsExactly(true, true)
        verify(exactly = 1) { verifier.verify(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a vote added WHEN isShown again THEN the operation is verified anew`() = runTest {
        // Arrange
        isShown()
        val withBobVote = OPERATION.copy(signatures = OPERATION.signatures + BOB_VOTE)

        // Act
        isShown(operation = withBobVote)

        // Assert
        verify(exactly = 1) { verifier.verify(any(), operation = withBobVote, any(), any(), any()) }
        verify(exactly = 2) { verifier.verify(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the smart gas list is unavailable WHEN isShown THEN hidden and asked again next time`() = runTest {
        // Arrange
        coEvery { gasTokenResolver.findUnpriced(network = any(), address = any()) } returns
            IOException("offline").left()

        // Act
        val whileUnpriced = isShown()
        coEvery { gasTokenResolver.findUnpriced(network = any(), address = any()) } returns
            SafeGasToken.Coin.right()
        val oncePriced = isShown()

        // Assert
        assertThat(listOf(whileUnpriced, oncePriced)).containsExactly(false, true).inOrder()
        verify(exactly = 1) { verifier.verify(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN a gas token outside smart gas WHEN isShown THEN it is hidden`() = runTest {
        // Arrange
        coEvery { gasTokenResolver.findUnpriced(network = any(), address = any()) } returns
            null.right()

        // Act
        val actual = isShown()

        // Assert
        assertThat(actual).isFalse()
        verify(exactly = 0) { verifier.verify(any(), any(), any(), any(), any()) }
    }

    private suspend fun isShown(operation: JointOperation = OPERATION): Boolean = gate.isShown(
        account = account,
        jointAccount = JOINT_ACCOUNT,
        operation = operation,
        network = network,
        transfer = transfer,
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val REFUND_RECEIVER = "0xfee0000000000000000000000000000000000001"
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")
        val DISPLAYED = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)

        val ALICE_VOTE = JointOperation.MemberSignature(
            address = ALICE,
            state = JointOperation.MemberSignature.State.SIGNED,
            at = "2026-09-07T10:00:00Z",
            signature = "0x" + "aa".repeat(n = 64) + "1b",
        )
        val BOB_VOTE = JointOperation.MemberSignature(
            address = BOB,
            state = JointOperation.MemberSignature.State.SIGNED,
            at = "2026-09-07T10:05:00Z",
            signature = "0x" + "bb".repeat(n = 64) + "1c",
        )

        val OPERATION = JointOperation(
            operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
            chainId = 137L,
            safeTx = SafeTx(
                to = RECIPIENT,
                value = AMOUNT,
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger("150000"),
                baseGas = SafeTxGasBuilder.SAFE_BASE_GAS,
                gasPrice = BigInteger("60000000000"),
                gasToken = SafeTxGasBuilder.NATIVE_GAS_TOKEN,
                refundReceiver = REFUND_RECEIVER,
                nonce = BigInteger.ZERO,
            ),
            status = JointOperation.Status.AWAITING_SIGNATURES,
            initiator = ALICE,
            signatures = listOf(ALICE_VOTE),
        )

        val JOINT_ACCOUNT = JointAccount(
            cryptoAccountId = "joint",
            membersCount = 2,
            threshold = 2,
            safeAddress = SAFE_ADDRESS,
            status = JointAccount.Status.ACTIVE,
            members = listOf(
                JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR),
                JointAccount.Member(name = "Bob", address = BOB, role = JointAccount.Role.MEMBER),
            ),
            source = StatusSource.ACTUAL,
        )
    }
}