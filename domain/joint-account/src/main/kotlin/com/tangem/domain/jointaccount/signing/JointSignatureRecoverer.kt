package com.tangem.domain.jointaccount.signing

import arrow.core.Either
import com.tangem.domain.jointaccount.model.SafeTx

/**
 * Recovers the signer of a signature the backend relays from another member, so its attribution is never trusted.
 *
 * Takes the `0x`-prefixed 65-byte R‖S‖V signature with `v ∈ {27, 28}` and returns the address in lower-case hex;
 * any other shape, or a signature that recovers to no point, is a `Left`.
 */
interface JointSignatureRecoverer {

    /** EIP-191 over the canonical JSON of [payload], the `/confirm` form */
    fun recoverPayloadSigner(payload: JointAccountSignablePayload, signature: String): Either<Throwable, String>

    /** EIP-712 over [safeTx] in the domain `{chainId, verifyingContract = safeAddress}`, the vote form */
    fun recoverSafeTxSigner(
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        signature: String,
    ): Either<Throwable, String>
}