package com.tangem.lib.crypto.models

data class ProxyFees(
    val minFee: ProxyFee,
    val normalFee: ProxyFee,
    val priorityFee: ProxyFee,
)