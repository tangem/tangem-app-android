package com.tangem.domain.jointaccount.verifier

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

/**
 * Signing authorises `(baseGas + gasUsed) * gasPrice` in `gasToken` to leave the Safe for `refundReceiver` on top
 * of the transfer on screen, so these fields move money and are checked field by field.
 *
 * Two of them carry a meaning a range check would miss, both read off `execTransaction`: a `gasPrice` of zero pays
 * no refund at all (`if (gasPrice > 0)`), so it is the safest value rather than a loophole, while a
 * `refundReceiver` of the zero address means `tx.origin` — whoever executes the operation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationRefundCheckTest {

    private val payloadCodec: JointTransferPayloadCodec = mockk()
    private val safeAddressCalculator: SafeAddressCalculator = mockk()

    private val verifier = JointOperationVerifier(
        payloadCodec = payloadCodec,
        safeAddressCalculator = safeAddressCalculator,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun checkRefund(model: RefundModel) {
        // Arrange
        every { safeAddressCalculator.compute(any(), any(), any()) } returns SAFE_ADDRESS.right()
        every { payloadCodec.decode(any()) } returns displayed
        every { payloadCodec.encode(displayed) } returns SafeInnerCall(
            to = RECIPIENT,
            value = AMOUNT,
            data = "0x",
        )

        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            operation = operation(safeTx = model.safeTx),
            displayed = displayed,
            ownerAddress = ALICE,
            refundBounds = JointOperationVerifier.RefundBounds(
                refundReceiver = REFUND_RECEIVER,
                networkGasPrice = NETWORK_GAS_PRICE,
            ),
        )

        // Assert
        val expected = if (model.isRefused) {
            VerificationFailed(reason = VerificationFailed.Reason.REFUND_MISMATCH).left()
        } else {
            Unit.right()
        }
        assertThat(actual).isEqualTo(expected)
    }

    internal data class RefundModel(val safeTx: SafeTx, val isRefused: Boolean)

    private fun provideTestModels() = listOf(
        RefundModel(safeTx = safeTx(), isRefused = false),
        // `execTransaction` pays nothing at all when the signed price is zero
        RefundModel(safeTx = safeTx(gasPrice = BigInteger.ZERO), isRefused = false),
        RefundModel(safeTx = safeTx(gasPrice = BigInteger.ONE.negate()), isRefused = true),
        RefundModel(safeTx = safeTx(gasPrice = NETWORK_GAS_PRICE * BigInteger.valueOf(21)), isRefused = true),
        // Not a recipient but an instruction to pay whoever executes the operation
        RefundModel(safeTx = safeTx(refundReceiver = ZERO_ADDRESS), isRefused = true),
        RefundModel(safeTx = safeTx(refundReceiver = ALICE), isRefused = true),
        RefundModel(safeTx = safeTx(gasToken = TOKEN), isRefused = true),
        RefundModel(safeTx = safeTx(baseGas = BigInteger.ONE), isRefused = true),
    )

    @Test
    fun `GIVEN the fee recipient itself is the zero address WHEN verified THEN the refund is still refused`() {
        // Arrange — the creation path only checks that the recipient is an address, and zero is one
        every { safeAddressCalculator.compute(any(), any(), any()) } returns SAFE_ADDRESS.right()
        every { payloadCodec.decode(any()) } returns displayed
        every { payloadCodec.encode(displayed) } returns SafeInnerCall(
            to = RECIPIENT,
            value = AMOUNT,
            data = "0x",
        )

        // Act
        val actual = verifier.verify(
            jointAccount = jointAccount(),
            operation = operation(safeTx = safeTx(refundReceiver = ZERO_ADDRESS)),
            displayed = displayed,
            ownerAddress = ALICE,
            refundBounds = JointOperationVerifier.RefundBounds(
                refundReceiver = ZERO_ADDRESS,
                networkGasPrice = NETWORK_GAS_PRICE,
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(
            VerificationFailed(reason = VerificationFailed.Reason.REFUND_MISMATCH).left(),
        )
    }

    private val displayed = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)

    private fun safeTx(
        gasPrice: BigInteger = NETWORK_GAS_PRICE,
        gasToken: String = ZERO_ADDRESS,
        refundReceiver: String = REFUND_RECEIVER,
        baseGas: BigInteger = BASE_GAS,
    ) = SafeTx(
        to = RECIPIENT,
        value = AMOUNT,
        data = "0x",
        operation = SafeTx.OPERATION_CALL,
        safeTxGas = BigInteger("150000"),
        baseGas = baseGas,
        gasPrice = gasPrice,
        gasToken = gasToken,
        refundReceiver = refundReceiver,
        nonce = BigInteger.ZERO,
    )

    private fun operation(safeTx: SafeTx) = JointOperation(
        operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
        chainId = 137L,
        safeTx = safeTx,
        status = JointOperation.Status.AWAITING_SIGNATURES,
        initiator = ALICE,
        signatures = emptyList(),
    )

    private fun jointAccount() = JointAccount(
        cryptoAccountId = "account",
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.ACTIVE,
        members = listOf(JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR)),
        source = StatusSource.ACTUAL,
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val REFUND_RECEIVER = "0xfee0000000000000000000000000000000000001"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")
        val NETWORK_GAS_PRICE: BigInteger = BigInteger("30000000000")
        val BASE_GAS: BigInteger = BigInteger("110000")
    }
}