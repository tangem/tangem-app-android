package com.tangem.domain.jointaccount.gas

/**
 * The address the Safe refunds the relayer to. The backend accepts exactly one — its treasury — and refuses an
 * operation naming any other, so the value is pinned here rather than read from the gasless service, whose fee
 * recipient is a different address.
 */
fun interface JointRefundReceiverProvider {

    suspend fun refundReceiver(): String

    companion object {
        const val TREASURY = "0x5F2C435DF8D00D0cF842a52Dd166B1Dcd69734C2"
    }
}