package com.tangem.blockchain.common.extensions

import com.tangem.blockchain.common.Amount
import java.math.BigInteger

fun Amount.bigIntegerValue(): BigInteger? {
    return this.value?.movePointRight(this.decimals.toInt())?.toBigInteger()
}