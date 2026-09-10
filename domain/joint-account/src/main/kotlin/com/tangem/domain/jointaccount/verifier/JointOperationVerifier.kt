package com.tangem.domain.jointaccount.verifier

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.Erc20TransferEncoder
import com.tangem.domain.jointaccount.safe.SafeAddress
import com.tangem.domain.jointaccount.safe.SafeSingleton
import java.math.BigInteger

/**
 * The local checks a co-signer runs before anything reaches the card — each blocks the signature on its own:
 *
 * 1. the Safe address resolves from the composition the app knows (CREATE2, any supported singleton);
 * 2. this wallet's own owner address is among the members;
 * 3. the `SafeTx` decodes to exactly what the UI shows the user — recipient, amount, asset;
 * 4. the payload has one of the two allowed shapes and never targets the Safe itself.
 */
class JointOperationVerifier {

    fun verify(
        jointAccount: JointAccount,
        operation: JointOperation,
        displayed: JointTransferIntent,
        ownerAddress: String,
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
    }

    private fun Raise<JointOperationSignError>.checkSafeAddress(jointAccount: JointAccount, safeAddress: String) {
        val owners = jointAccount.members.map(JointAccount.Member::address)

        val isAnySingletonMatching = SafeSingleton.entries.any { singleton ->
            SafeAddress.compute(owners = owners, threshold = jointAccount.threshold, singleton = singleton)
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

    private fun Raise<JointOperationSignError>.checkDisplayedPayload(safeTx: SafeTx, displayed: JointTransferIntent) {
        val isMatching = when (displayed) {
            is JointTransferIntent.Native ->
                safeTx.to.equals(displayed.recipient, ignoreCase = true) &&
                    safeTx.value == displayed.amountWei &&
                    normalized(safeTx.data) == EMPTY_CALLDATA
            is JointTransferIntent.Erc20 ->
                safeTx.to.equals(displayed.tokenContract, ignoreCase = true) &&
                    safeTx.value == BigInteger.ZERO &&
                    normalized(safeTx.data) == displayed.encodedTransferOrNull()
        }

        ensure(isMatching) { VerificationFailed(reason = VerificationFailed.Reason.PAYLOAD_MISMATCH) }
    }

    private fun Raise<JointOperationSignError>.checkForm(safeTx: SafeTx, safeAddress: String) {
        ensure(!safeTx.to.equals(safeAddress, ignoreCase = true)) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }
        ensure(safeTx.operation == SafeTx.OPERATION_CALL) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }

        val calldata = normalized(safeTx.data)
        val isNative = safeTx.value > BigInteger.ZERO && calldata == EMPTY_CALLDATA
        val isErc20Transfer = safeTx.value == BigInteger.ZERO &&
            calldata.startsWith(prefix = TRANSFER_CALLDATA_PREFIX) &&
            calldata.length == TRANSFER_CALLDATA_LENGTH

        ensure(isNative || isErc20Transfer) {
            VerificationFailed(reason = VerificationFailed.Reason.FORBIDDEN_FORM)
        }
    }

    /** `null` when the displayed transfer cannot be encoded at all — it then matches no calldata */
    private fun JointTransferIntent.Erc20.encodedTransferOrNull(): String? = runCatching {
        Erc20TransferEncoder.encodeTransfer(recipient = recipient, amount = amountWei)
    }.getOrNull()

    private fun normalized(data: String): String = "0x" + data.lowercase().removePrefix(prefix = "0x")

    private companion object {
        const val EMPTY_CALLDATA = "0x"

        // "0x" + transfer(address,uint256) selector + two 32-byte ABI words
        const val TRANSFER_CALLDATA_PREFIX = "0xa9059cbb"
        const val TRANSFER_CALLDATA_LENGTH = 2 + 8 + 64 + 64
    }
}