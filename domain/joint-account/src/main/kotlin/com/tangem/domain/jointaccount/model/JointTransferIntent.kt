package com.tangem.domain.jointaccount.model

import java.math.BigInteger

/** What the user asked to send, as shown on the screen — the use case turns it into the Safe inner call */
sealed interface JointTransferIntent {

    val recipient: String
    val amountWei: BigInteger

    /** A native coin transfer: the Safe calls the recipient with the value and empty calldata */
    data class Native(
        override val recipient: String,
        override val amountWei: BigInteger,
    ) : JointTransferIntent

    /** An ERC-20 transfer: the Safe calls [tokenContract] with `transfer(recipient, amount)` and zero value */
    data class Erc20(
        val tokenContract: String,
        override val recipient: String,
        override val amountWei: BigInteger,
    ) : JointTransferIntent
}