package com.tangem.data.jointaccount.verifier

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.data.jointaccount.safe.DefaultJointTransferPayloadCodec
import com.tangem.data.jointaccount.safe.DefaultSafeAddressCalculator
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.verifier.JointOperationVerifier
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationVerifierTest {

    private val verifier = JointOperationVerifier(
        payloadCodec = DefaultJointTransferPayloadCodec(),
        safeAddressCalculator = DefaultSafeAddressCalculator(),
    )

    @Test
    fun `GIVEN operation matching what the user sees WHEN verify THEN the signature is allowed`() = runTest {
        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            ownerAddress = ALICE,
            operation = operation(safeTx = nativeSafeTx()),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    /**
     * `execTransaction` guards the refund with `if (gasPrice > 0)`, so a signed zero pays nothing at all — the
     * safest value there is, and refusing it would refuse an operation that costs the Safe no refund.
     */
    @Test
    fun `GIVEN a zero gas price WHEN verify THEN the signature is allowed because no refund is paid`() = runTest {
        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            ownerAddress = ALICE,
            operation = operation(safeTx = nativeSafeTx().copy(gasPrice = BigInteger.ZERO)),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    @Test
    fun `GIVEN the zero receiver spelled without the prefix WHEN verify THEN the refund is still refused`() = runTest {
        // Arrange
        val rawZero = ZERO_ADDRESS.removePrefix(prefix = "0x")

        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            ownerAddress = ALICE,
            operation = operation(safeTx = nativeSafeTx().copy(refundReceiver = rawZero)),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(refundReceiver = rawZero),
        )

        // Assert
        assertThat(actual).isEqualTo(
            VerificationFailed(reason = VerificationFailed.Reason.REFUND_MISMATCH).left(),
        )
    }

    /** The fee recipient is only checked for being an address by creation, and the zero address is one */
    @Test
    fun `GIVEN the configured fee recipient is itself zero WHEN verify THEN the refund is still refused`() = runTest {
        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            ownerAddress = ALICE,
            operation = operation(safeTx = nativeSafeTx().copy(refundReceiver = ZERO_ADDRESS)),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(refundReceiver = ZERO_ADDRESS),
        )

        // Assert
        assertThat(actual).isEqualTo(
            VerificationFailed(reason = VerificationFailed.Reason.REFUND_MISMATCH).left(),
        )
    }

    @Test
    fun `GIVEN erc20 operation matching what the user sees WHEN verify THEN the signature is allowed`() = runTest {
        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            ownerAddress = ALICE,
            operation = operation(safeTx = erc20SafeTx()),
            displayed = JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    @Test
    fun `GIVEN calldata with an uppercase 0X prefix WHEN verify THEN it is normalized like lowercase`() = runTest {
        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            ownerAddress = ALICE,
            operation = operation(safeTx = nativeSafeTx(data = "0X")),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun negativeBranches(model: NegativeModel) = runTest {
        // Act
        val actual = verifier.verify(
            jointAccount = model.jointAccount,
            operation = operation(safeTx = model.safeTx),
            displayed = model.displayed,
            refundBounds = model.refundBounds,
            ownerAddress = model.ownerAddress,
        )

        // Assert
        assertThat(actual).isEqualTo(VerificationFailed(reason = model.expectedReason).left())
    }

    internal data class NegativeModel(
        val name: String,
        val jointAccount: JointAccount,
        val safeTx: SafeTx,
        val displayed: JointTransferIntent,
        val refundBounds: JointOperationVerifier.RefundBounds = refundBounds(),
        val ownerAddress: String = ALICE,
        val expectedReason: VerificationFailed.Reason,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        NegativeModel(
            name = "calldata longer than any allowed form is rejected before the other checks",
            jointAccount = jointAccount(safeAddress = null),
            safeTx = erc20SafeTx().let { it.copy(data = it.data + "00") },
            displayed = JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.FORBIDDEN_FORM,
        ),
        NegativeModel(
            name = "safe address is not the composition's CREATE2",
            jointAccount = jointAccount(safeAddress = FOREIGN_SAFE),
            safeTx = nativeSafeTx(),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH,
        ),
        NegativeModel(
            name = "no safe address on the account",
            jointAccount = jointAccount(safeAddress = null),
            safeTx = nativeSafeTx(),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH,
        ),
        NegativeModel(
            name = "own owner address is not among the members",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx(),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            ownerAddress = MALLORY,
            expectedReason = VerificationFailed.Reason.NOT_A_MEMBER,
        ),
        NegativeModel(
            name = "native amount differs from what the user sees",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx(),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT + BigInteger.ONE),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.PAYLOAD_MISMATCH,
        ),
        NegativeModel(
            name = "native recipient differs from what the user sees",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx(),
            displayed = JointTransferIntent.Native(recipient = MALLORY, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.PAYLOAD_MISMATCH,
        ),
        NegativeModel(
            name = "erc20 calldata sends to someone else",
            jointAccount = jointAccount(),
            safeTx = erc20SafeTx(calldataRecipient = MALLORY),
            displayed = JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.PAYLOAD_MISMATCH,
        ),
        NegativeModel(
            name = "displayed erc20 amount cannot be encoded — mismatch, not a crash",
            jointAccount = jointAccount(),
            safeTx = erc20SafeTx(),
            displayed = JointTransferIntent.Erc20(
                tokenContract = TOKEN,
                recipient = RECIPIENT,
                amountWei = BigInteger.ONE.shiftLeft(256),
            ),
            expectedReason = VerificationFailed.Reason.PAYLOAD_MISMATCH,
        ),
        NegativeModel(
            name = "the refund would go to someone other than the fee recipient",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx().copy(refundReceiver = MALLORY),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            expectedReason = VerificationFailed.Reason.REFUND_MISMATCH,
        ),
        NegativeModel(
            name = "the refund would be paid to whoever executes the operation",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx().copy(refundReceiver = ZERO_ADDRESS),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            expectedReason = VerificationFailed.Reason.REFUND_MISMATCH,
        ),
        NegativeModel(
            name = "a gas price that is no uint256 at all",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx().copy(gasPrice = BigInteger.ONE.negate()),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            expectedReason = VerificationFailed.Reason.REFUND_MISMATCH,
        ),
        NegativeModel(
            name = "the refund would be paid in a token instead of the native coin",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx().copy(gasToken = TOKEN),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            expectedReason = VerificationFailed.Reason.REFUND_MISMATCH,
        ),
        NegativeModel(
            name = "baseGas is not one of the two values creation can put there",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx().copy(baseGas = BigInteger("9000000")),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            expectedReason = VerificationFailed.Reason.REFUND_MISMATCH,
        ),
        NegativeModel(
            name = "gasPrice is absurd next to the network price",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx().copy(gasPrice = NETWORK_GAS_PRICE * BigInteger.valueOf(21)),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            expectedReason = VerificationFailed.Reason.REFUND_MISMATCH,
        ),
        NegativeModel(
            name = "operation targets the safe itself",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx(to = COMPUTED_SAFE),
            displayed = JointTransferIntent.Native(recipient = COMPUTED_SAFE, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.FORBIDDEN_FORM,
        ),
        NegativeModel(
            name = "calldata is neither empty nor an erc20 transfer",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx(data = "0xdeadbeef", value = BigInteger.ZERO),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.PAYLOAD_MISMATCH,
        ),
        NegativeModel(
            name = "delegatecall is never signed",
            jointAccount = jointAccount(),
            safeTx = nativeSafeTx(operation = 1),
            displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            refundBounds = refundBounds(),
            expectedReason = VerificationFailed.Reason.FORBIDDEN_FORM,
        ),
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val BOB = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"
        const val MALLORY = "0x3333333333333333333333333333333333333333"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val FOREIGN_SAFE = "0x5afe000000000000000000000000000000000001"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

        /** What creation puts into an operation: the zero address would be an instruction to pay `tx.origin` */
        const val FEE_RECIPIENT = "0xfee0000000000000000000000000000000000001"
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")

        /** The fixtures carry ×2 of this, the cap creation applies */
        val NETWORK_GAS_PRICE: BigInteger = BigInteger("30000000000")

        val COMPUTED_SAFE: String = DefaultSafeAddressCalculator()
            .compute(owners = listOf(ALICE, BOB), threshold = 2, singleton = SafeSingleton.SAFE_L2)
            .getOrNull()!!

        fun jointAccount(safeAddress: String? = COMPUTED_SAFE): JointAccount = JointAccount(
            cryptoAccountId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
            membersCount = 2,
            threshold = 2,
            safeAddress = safeAddress,
            status = JointAccount.Status.ACTIVE,
            members = listOf(
                JointAccount.Member(name = "Alice", address = ALICE.lowercase(), role = JointAccount.Role.CREATOR),
                JointAccount.Member(name = "Bob", address = BOB.lowercase(), role = JointAccount.Role.MEMBER),
            ),
            source = StatusSource.ACTUAL,
        )

        /** Matches what the fixtures carry, so a test that is not about the refund passes it unchanged */
        fun refundBounds(
            refundReceiver: String = FEE_RECIPIENT,
            networkGasPrice: BigInteger = NETWORK_GAS_PRICE,
        ) = JointOperationVerifier.RefundBounds(
            refundReceiver = refundReceiver,
            networkGasPrice = networkGasPrice,
        )

        fun operation(safeTx: SafeTx): JointOperation = JointOperation(
            operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
            chainId = 137,
            safeTx = safeTx,
            status = JointOperation.Status.AWAITING_SIGNATURES,
            initiator = ALICE.lowercase(),
            signatures = emptyList(),
        )

        fun nativeSafeTx(
            to: String = RECIPIENT,
            value: BigInteger = AMOUNT,
            data: String = "0x",
            operation: Int = SafeTx.OPERATION_CALL,
        ): SafeTx = SafeTx(
            to = to,
            value = value,
            data = data,
            operation = operation,
            safeTxGas = BigInteger("150000"),
            baseGas = BigInteger("110000"),
            gasPrice = BigInteger("60000000000"),
            gasToken = ZERO_ADDRESS,
            refundReceiver = FEE_RECIPIENT,
            nonce = BigInteger.ZERO,
        )

        fun transferCalldata(recipient: String, amount: BigInteger): String = requireNotNull(
            DefaultJointTransferPayloadCodec().encode(
                JointTransferIntent.Erc20(
                    tokenContract = "0x" + "1".repeat(n = 40),
                    recipient = recipient,
                    amountWei = amount,
                ),
            ),
        ).data

        fun erc20SafeTx(calldataRecipient: String = RECIPIENT): SafeTx = nativeSafeTx(
            to = TOKEN,
            value = BigInteger.ZERO,
            data = transferCalldata(recipient = calldataRecipient, amount = AMOUNT),
        )
    }
}