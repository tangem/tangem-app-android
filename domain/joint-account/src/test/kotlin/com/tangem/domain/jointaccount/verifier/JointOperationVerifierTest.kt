package com.tangem.domain.jointaccount.verifier

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.models.StatusSource
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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

    private val verifier = JointOperationVerifier(
        payloadCodec = payloadCodec,
        safeAddressCalculator = safeAddressCalculator,
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
        signatures = emptyList(),
    )

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
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
            members = listOf(JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR)),
            source = StatusSource.ACTUAL,
            inFlightOperations = emptyMap(),
        )
    }
}