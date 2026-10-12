package com.tangem.domain.jointaccount.model

/** Why co-signing a joint operation stopped. Every variant blocks the signature — none is skippable. */
sealed interface JointOperationSignError {

    /** No operation waits in the (account, network) slot — it finished or was removed while the sheet was open */
    data object OperationNotFound : JointOperationSignError

    /** One of the local security checks failed — the operation must not be signed */
    data class VerificationFailed(val reason: Reason) : JointOperationSignError {

        enum class Reason {
            /** The Safe address does not resolve from the composition the app knows */
            SAFE_ADDRESS_MISMATCH,

            /** This wallet's own owner address is not among the members */
            NOT_A_MEMBER,

            /** The decoded `SafeTx` differs from what the UI shows the user */
            PAYLOAD_MISMATCH,

            /** The payload is not one of the two allowed transfer shapes, or targets the Safe itself */
            FORBIDDEN_FORM,

            /** The wallet's owner key for the account is not derived — nothing to check membership with */
            OWNER_KEY_NOT_DERIVED,

            /** The deployed Safe answers a different composition than the one the app knows */
            COMPOSITION_MISMATCH,

            /**
             * The refund fields would pay someone or something other than what the operation was built to pay.
             * Signing authorises `(baseGas + gasUsed) * gasPrice` in `gasToken` to leave the Safe for
             * `refundReceiver` on top of the transfer the user sees, so these four move money.
             */
            REFUND_MISMATCH,
        }
    }

    /** Blockaid marked the payload malicious — signing is blocked, not warned about */
    data object Malicious : JointOperationSignError

    /** The threshold is already reached — this signature is not needed */
    data object ThresholdReached : JointOperationSignError

    /** The user dismissed the signing dialog */
    data object UserCancelled : JointOperationSignError

    data class Failed(val cause: Throwable) : JointOperationSignError
}