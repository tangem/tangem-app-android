package com.tangem.lib.visa.utils

import org.joda.time.Instant
import java.math.BigDecimal
import java.math.BigInteger

internal fun BigInteger.toBigDecimal(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals)
}

internal fun BigInteger.toInstant(): Instant {
    return Instant.ofEpochSecond(toLong())
}