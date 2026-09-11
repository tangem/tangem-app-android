package com.tangem.domain.jointaccount.safe

import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx

/**
 * Translates a transfer between the form the app speaks in and the payload the Safe executes.
 *
 * The bytes are the blockchain SDK's business, so the implementation lives in the data layer and this domain only

 * [encode] and read back by [decode], and a co-signer's verification compares one against the other — a difference
 * between two implementations would be a hole rather than a bug.
 */
interface JointTransferPayloadCodec {

    /** The inner call the Safe must run for [intent]; `null` when the intent cannot be encoded at all */
    fun encode(intent: JointTransferIntent): SafeInnerCall?

    /**
     * The transfer [safeTx] carries: a call with empty calldata and a non-zero value moves the network coin, a
     * `transfer(recipient, amount)` call moves the token deployed at the call target. `null` for anything else —
     * an unknown calldata, a zero-value plain call, a token transfer that also moves value.
     */
    fun decode(safeTx: SafeTx): JointTransferIntent?
}