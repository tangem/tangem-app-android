package com.tangem.lib.crypto.models

import java.math.BigInteger

data class ProxyFee(
    val gasLimit: BigInteger,
    val fee: ProxyAmount,
)
