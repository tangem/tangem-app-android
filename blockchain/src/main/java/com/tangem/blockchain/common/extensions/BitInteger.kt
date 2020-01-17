package com.tangem.blockchain.common.extensions

import org.bitcoinj.core.ECKey
import java.math.BigInteger

fun BigInteger.toCanonicalised(): BigInteger {
    if (!this.isCanonical()) ECKey.CURVE.n - this
    return this
}

fun BigInteger.isCanonical(): Boolean = this <= ECKey.HALF_CURVE_ORDER