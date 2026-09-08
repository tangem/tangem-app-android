package com.tangem.data.jointaccount.converter

import java.math.BigInteger

/** [BigInteger.longValueExact] appears on Android only at API 31, the app's minSdk is 26 */
internal fun BigInteger.longValueExactCompat(): Long {
    if (bitLength() >= Long.SIZE_BITS) throw ArithmeticException("BigInteger out of long range")

    return toLong()
}