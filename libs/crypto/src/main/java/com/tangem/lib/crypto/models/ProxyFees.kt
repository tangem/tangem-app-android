package com.tangem.lib.crypto.models

sealed class ProxyFees {

    data class MultipleFees(
        val minFee: ProxyFee,
        val normalFee: ProxyFee,
        val priorityFee: ProxyFee,
    ) : ProxyFees()

    data class SingleFee(
        val singleFee: ProxyFee,
    ) : ProxyFees()
}