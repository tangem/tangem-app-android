package com.tangem.domain.jointaccount.model

import java.math.BigInteger

/**
 * The Safe transaction exactly as it is signed and executed: every field is covered by the EIP-712
 * signature, so none of them can change without collecting the signatures again.
 *
 * @property to             the address the Safe calls: the recipient for a native transfer, the token
 * contract for an ERC-20 one
 * @property value          the native amount the Safe sends with the call, in wei
 * @property data           the calldata of the inner call as a `0x`-prefixed hex string; `0x` when empty

 * @property safeTxGas      the gas limit of the inner call
 * @property baseGas        the gas the Safe machinery itself burns, deployment included
 * @property gasPrice       the gas price the account refunds the executor at
 * @property gasToken       the token the refund is paid in; `address(0)` means the native coin
 * @property refundReceiver the address the refund is paid to
 * @property nonce          the Safe contract nonce the transaction is bound to
 */
data class SafeTx(
    val to: String,
    val value: BigInteger,
    val data: String,
    val operation: Int,
    val safeTxGas: BigInteger,
    val baseGas: BigInteger,
    val gasPrice: BigInteger,
    val gasToken: String,
    val refundReceiver: String,
    val nonce: BigInteger,
) {

    companion object {
        /** The only Safe operation type the app creates or signs: a plain CALL */
        const val OPERATION_CALL = 0
    }
}

/**
 * The inner call a [SafeTx] wraps — what the Safe itself executes. Phase 2 sources (swap, WalletConnect)
 * plug in their own callData here without touching the gas and signing paths.
 */
data class SafeInnerCall(
    val to: String,
    val value: BigInteger,
    val data: String,
)