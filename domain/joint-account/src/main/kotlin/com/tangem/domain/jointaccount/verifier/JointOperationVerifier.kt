package com.tangem.domain.jointaccount.verifier

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.safe.SafeSingleton
import java.math.BigInteger

/**
 * The local checks a co-signer runs before anything reaches the card — each blocks the signature on its own:
 *
 * 1. the Safe address resolves from the composition the app knows (CREATE2, any supported singleton);
 * 2. this wallet's own owner address is among the members;
 * 3. the `SafeTx` decodes to exactly what the UI shows the user — recipient, amount, asset;
 * 4. the payload has one of the two allowed shapes and never targets the Safe itself;
 * 5. the refund fields pay what the operation was built to pay, and no one else.
 */
class JointOperationVerifier(
    private val payloadCodec: JointTransferPayloadCodec,
    private val safeAddressCalculator: SafeAddressCalculator,
) {

    /**
     * What the refund fields are allowed to say, read from the same sources the creation path used.
     *
     * @param refundReceiver the fee recipient `SafeTxGasBuilder` puts into an operation it creates
     * @param networkGasPrice the current gas price of the network, the reference the ceiling is taken from
     */
    data class RefundBounds(val refundReceiver: String, val networkGasPrice: BigInteger)

    fun verify(
        jointAccount: JointAccount,
        operation: JointOperation,
        displayed: JointTransferIntent,
        ownerAddress: String,
        refundBounds: RefundBounds,
    ): Either<JointOperationSignError, Unit> = either {
        // Nothing allowed is longer than a transfer call: an oversized payload fails before anything touches it
        ensure(operation.safeTx.data.length <= TRANSFER_CALLDATA_LENGTH) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }
        val safeAddress = ensureNotNull(jointAccount.safeAddress) {
            VerificationFailed(reason = VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH)
        }

        checkSafeAddress(jointAccount = jointAccount, safeAddress = safeAddress)
        checkMembership(ownerAddress = ownerAddress, jointAccount = jointAccount)
        checkDisplayedPayload(safeTx = operation.safeTx, displayed = displayed)
        checkForm(safeTx = operation.safeTx, safeAddress = safeAddress)
        checkRefund(safeTx = operation.safeTx, bounds = refundBounds)
    }

    /**
     * The Safe pays the refund out of its own balance, so a co-signer who ignores these fields signs away funds
     * beyond the transfer on screen. Each field is compared to what the creation path would have put there:
     * the native coin, our fee recipient, and one of the two known `baseGas` values. `gasPrice` is only capped

     *
     * Two of the fields mean something to the contract that a range check alone would miss. `refundReceiver` of
     * the zero address is not a recipient but an instruction to pay `tx.origin`, so whoever executes the
     * operation is paid — refused outright, because the fee recipient we compare against is only checked for
     * being an address and could itself be zero by misconfiguration. A negative `gasPrice` is no uint256 at all
     * and would otherwise pass the ceiling and fail later as a generic signing error instead of a refund
     * mismatch. Zero is left alone deliberately: `execTransaction` pays nothing when `gasPrice` is zero.
     */
    private fun Raise<JointOperationSignError>.checkRefund(safeTx: SafeTx, bounds: RefundBounds) {
        val refundMismatch = VerificationFailed(reason = VerificationFailed.Reason.REFUND_MISMATCH)

        // Spelled the same way the digest normalizes them: a receiver without the 0x prefix is still an address
        val refundReceiver = normalized(safeTx.refundReceiver)
        ensure(normalized(safeTx.gasToken) == SafeTxGasBuilder.NATIVE_GAS_TOKEN) { refundMismatch }
        ensure(refundReceiver != ZERO_ADDRESS) { refundMismatch }
        ensure(refundReceiver == normalized(bounds.refundReceiver)) { refundMismatch }
        ensure(safeTx.baseGas in ALLOWED_BASE_GAS) { refundMismatch }
        ensure(safeTx.gasPrice.signum() >= 0) { refundMismatch }
        ensure(safeTx.gasPrice <= bounds.networkGasPrice * GAS_PRICE_CEILING_MULTIPLIER) { refundMismatch }
    }

    private fun Raise<JointOperationSignError>.checkSafeAddress(jointAccount: JointAccount, safeAddress: String) {
        val owners = jointAccount.members.map(JointAccount.Member::address)

        val isAnySingletonMatching = SafeSingleton.entries.any { singleton ->
            safeAddressCalculator.compute(owners = owners, threshold = jointAccount.threshold, singleton = singleton)
                .getOrNull()
                ?.equals(safeAddress, ignoreCase = true) == true
        }

        ensure(isAnySingletonMatching) {
            VerificationFailed(reason = VerificationFailed.Reason.SAFE_ADDRESS_MISMATCH)
        }
    }

    private fun Raise<JointOperationSignError>.checkMembership(ownerAddress: String, jointAccount: JointAccount) {
        val isMember = jointAccount.members.any { member ->
            member.address.equals(ownerAddress, ignoreCase = true)
        }

        ensure(isMember) { VerificationFailed(reason = VerificationFailed.Reason.NOT_A_MEMBER) }
    }

    /**

     * to what the operation actually carries. Re-encoding rather than decoding is deliberate: it is the payload
     * bytes that get signed, so they are what must match.
     */
    private fun Raise<JointOperationSignError>.checkDisplayedPayload(safeTx: SafeTx, displayed: JointTransferIntent) {
        val expected = payloadCodec.encode(displayed)

        val isMatching = expected != null &&
            safeTx.to.equals(expected.to, ignoreCase = true) &&
            safeTx.value == expected.value &&
            normalized(safeTx.data) == normalized(expected.data)

        ensure(isMatching) { VerificationFailed(reason = VerificationFailed.Reason.PAYLOAD_MISMATCH) }
    }

    private fun Raise<JointOperationSignError>.checkForm(safeTx: SafeTx, safeAddress: String) {
        ensure(!safeTx.to.equals(safeAddress, ignoreCase = true)) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }
        ensure(safeTx.operation == SafeTx.OPERATION_CALL) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }

        // The two allowed shapes are exactly the ones the codec can read back
        ensure(payloadCodec.decode(safeTx) != null) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }
    }

    private fun normalized(data: String): String = "0x" + data.lowercase().removePrefix(prefix = "0x")

    private companion object {
        // "0x" + transfer(address,uint256) selector + two 32-byte ABI words — nothing allowed is longer
        const val TRANSFER_CALLDATA_LENGTH = 2 + 8 + 64 + 64

        /** The two values `SafeTxGasBuilder` can put there: with and without the Safe deployment */
        val ALLOWED_BASE_GAS = setOf(
            SafeTxGasBuilder.SAFE_BASE_GAS,
            SafeTxGasBuilder.SAFE_BASE_GAS + SafeTxGasBuilder.DEPLOY_GAS,
        )

        /**
         * Creation caps `gasPrice` at ×2 of the network price. The ceiling here is deliberately far above that:
         * an operation collecting signatures for hours carries the price of the moment it was built, and the
         * decision on a risen price is to execute it as is. What this rejects is an absurd value, not drift.
         */
        val GAS_PRICE_CEILING_MULTIPLIER: BigInteger = BigInteger.valueOf(20)

        /** `refundReceiver` of this value is not a recipient but an instruction to pay `tx.origin` */
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}