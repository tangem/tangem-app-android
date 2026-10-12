package com.tangem.domain.jointaccount.gas

/**
 * The address the Safe refunds the relayer to. The backend accepts exactly one — the treasury of its environment —
 * and refuses an operation naming any other, so the value is pinned per environment rather than read from the gasless
 * service, whose fee recipient is a different address.
 */
fun interface JointRefundReceiverProvider {

    suspend fun refundReceiver(): String
}