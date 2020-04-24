package com.tangem.blockchain.extensions

import com.tangem.blockchain.common.Amount
import java.math.BigInteger

fun Amount.bigIntegerValue(): BigInteger? {
    return this.value?.movePointRight(this.decimals.toInt())?.toBigInteger()
}

fun Amount.isAboveZero(): Boolean {
    val value = this.value ?: return false
    return value.signum() == 1
}