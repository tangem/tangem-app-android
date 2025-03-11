package com.tangem.utils.extensions

import java.math.BigDecimal

/** Returns [BigDecimal] or [BigDecimal.ZERO] if [this] is null */
fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

/** Checks if [this] is [BigDecimal] */
fun BigDecimal.isZero(): Boolean = this.compareTo(BigDecimal.ZERO) == 0

/** Checks if [this] is positive */
fun BigDecimal.isPositive(): Boolean = this.signum() == 1

/** Removes trailing zeros and returns plain [String] */
fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()

/** Compares two [BigDecimal] numbers */
infix fun BigDecimal.isEqualTo(other: BigDecimal): Boolean = this.compareTo(other) == 0