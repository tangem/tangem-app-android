package com.tangem.domain.jointaccount.verifier

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperation.MemberSignature.State
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.signing.JointSignatureRecoverer
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationVerifierTest {

    private val payloadCodec: JointTransferPayloadCodec = mockk {
        every { encode(DISPLAYED) } returns SafeInnerCall(to = RECIPIENT, value = AMOUNT, data = "0x")
        every { decode(any()) } returns DISPLAYED
    }
    private val safeAddressCalculator: SafeAddressCalculator = mockk {
        every { compute(owners = any(), threshold = any(), singleton = any()) } returns SAFE_ADDRESS.right()
    }

    private val signatureRecoverer: JointSignatureRecoverer = mockk {
        every { recoverSafeTxSigner(safeTx = any(), chainId = any(), safeAddress = any(), signature = any()) } answers {
            SIGNERS[arg<String>(3)]?.right() ?: IllegalArgumentException("not a signature").left()
        }
    }

    private val verifier = JointOperationVerifier(
        payloadCodec = payloadCodec,
        safeAddressCalculator = safeAddressCalculator,
        signatureRecoverer = signatureRecoverer,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN operation gas price WHEN verify THEN capped at four times the network price`(model: GasPriceModel) {
        // Act
        val actual = verifier.verify(
            jointAccount = JOINT_ACCOUNT,
            operation = operation(gasPrice = model.gasPrice),
            displayed = DISPLAYED,
            ownerAddress = ALICE,
            refundBounds = JointOperationVerifier.RefundBounds(
                refundReceiver = REFUND_RECEIVER,
                networkGasPrice = NETWORK_GAS_PRICE,
                gasToken = SafeGasToken.Coin,
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        GasPriceModel(gasPrice = NETWORK_GAS_PRICE * BigInteger.TWO, expected = Unit.right()),
        GasPriceModel(gasPrice = NETWORK_GAS_PRICE * BigInteger.valueOf(4), expected = Unit.right()),
        GasPriceModel(
            gasPrice = NETWORK_GAS_PRICE * BigInteger.valueOf(4) + BigInteger.ONE,
            expected = VerificationFailed(reason = VerificationFailed.Reason.REFUND_MISMATCH).left(),
        ),
    )

    data class GasPriceModel(val gasPrice: BigInteger, val expected: Either<JointOperationSignError, Unit>)

    @ParameterizedTest
    @MethodSource("provideVotesModels")
    fun `GIVEN relayed votes WHEN verify THEN every approve must recover to its member`(model: VotesModel) {
        // Act
        val actual = verifier.verify(
            jointAccount = JOINT_ACCOUNT,
            operation = operation(gasPrice = NETWORK_GAS_PRICE).copy(
                initiator = model.initiator,
                signatures = model.votes,
            ),
            displayed = DISPLAYED,
            ownerAddress = ALICE,
            refundBounds = JointOperationVerifier.RefundBounds(
                refundReceiver = REFUND_RECEIVER,
                networkGasPrice = NETWORK_GAS_PRICE,
                gasToken = SafeGasToken.Coin,
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideVotesModels() = listOf(
        VotesModel(votes = listOf(ALICE_VOTE), expected = Unit.right()),
        VotesModel(votes = listOf(ALICE_VOTE, vote(BOB, State.SIGNED, BOB_SIGNATURE)), expected = Unit.right()),
        VotesModel(votes = listOf(ALICE_VOTE, vote(BOB, State.DECLINED, signature = null)), expected = Unit.right()),
        VotesModel(votes = listOf(ALICE_VOTE, vote(BOB, State.UNKNOWN, signature = null)), expected = Unit.right()),
        VotesModel(votes = emptyList(), initiator = null, expected = Unit.right()),
        VotesModel(votes = listOf(vote(BOB, State.SIGNED, BOB_SIGNATURE)), initiator = null, expected = Unit.right()),
        VotesModel(votes = emptyList(), expected = VOTE_MISMATCH.left()),
        VotesModel(votes = listOf(vote(BOB, State.SIGNED, BOB_SIGNATURE)), expected = VOTE_MISMATCH.left()),
        VotesModel(votes = listOf(vote(ALICE, State.DECLINED, signature = null)), expected = VOTE_MISMATCH.left()),
        VotesModel(votes = listOf(vote(ALICE, State.SIGNED, signature = null)), expected = VOTE_MISMATCH.left()),
        VotesModel(votes = listOf(vote(ALICE, State.SIGNED, BOB_SIGNATURE)), expected = VOTE_MISMATCH.left()),
        VotesModel(
            votes = listOf(ALICE_VOTE, vote(BOB, State.SIGNED, signature = null)),
            expected = VOTE_MISMATCH.left(),
        ),
        VotesModel(
            votes = listOf(ALICE_VOTE, vote(BOB, State.SIGNED, ALICE_SIGNATURE)),
            expected = VOTE_MISMATCH.left(),
        ),
        VotesModel(votes = listOf(ALICE_VOTE, vote(BOB, State.SIGNED, "0xgarbage")), expected = VOTE_MISMATCH.left()),
        VotesModel(
            votes = listOf(ALICE_VOTE, vote(MALLORY, State.SIGNED, MALLORY_SIGNATURE)),
            expected = VOTE_MISMATCH.left(),
        ),
    )

    data class VotesModel(
        val votes: List<JointOperation.MemberSignature>,
        val initiator: String? = ALICE,
        val expected: Either<JointOperationSignError, Unit>,
    )

    private fun vote(address: String, state: State, signature: String?) = JointOperation.MemberSignature(
        address = address,
        state = state,
        at = "2026-09-07T10:00:00Z",
        signature = signature,
    )

    private fun operation(gasPrice: BigInteger) = JointOperation(
        operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
        chainId = 137L,
        safeTx = SafeTx(
            to = RECIPIENT,
            value = AMOUNT,
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger("150000"),
            baseGas = SafeTxGasBuilder.SAFE_BASE_GAS,
            gasPrice = gasPrice,
            gasToken = SafeTxGasBuilder.NATIVE_GAS_TOKEN,
            refundReceiver = REFUND_RECEIVER,
            nonce = BigInteger.ZERO,
        ),
        status = JointOperation.Status.AWAITING_SIGNATURES,
        initiator = ALICE,
        signatures = listOf(ALICE_VOTE),
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        const val MALLORY = "0x3333333333333333333333333333333333333333"
        val ALICE_SIGNATURE = "0x" + "aa".repeat(n = 64) + "1b"
        val BOB_SIGNATURE = "0x" + "bb".repeat(n = 64) + "1c"
        val MALLORY_SIGNATURE = "0x" + "cc".repeat(n = 64) + "1b"
        val SIGNERS = mapOf(ALICE_SIGNATURE to ALICE, BOB_SIGNATURE to BOB, MALLORY_SIGNATURE to MALLORY)
        val ALICE_VOTE = JointOperation.MemberSignature(
            address = ALICE,
            state = State.SIGNED,
            at = "2026-09-07T10:00:00Z",
            signature = ALICE_SIGNATURE,
        )
        val VOTE_MISMATCH = VerificationFailed(reason = VerificationFailed.Reason.VOTE_SIGNATURE_MISMATCH)
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val REFUND_RECEIVER = "0xfee0000000000000000000000000000000000001"
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")
        val NETWORK_GAS_PRICE: BigInteger = BigInteger("30000000000")
        val DISPLAYED = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)

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
            inFlightOperations = emptyMap(),
        )
    }
}