package com.tangem.domain.jointaccount.gas

fun interface JointRefundReceiverProvider {

    suspend fun refundReceiver(): String
}