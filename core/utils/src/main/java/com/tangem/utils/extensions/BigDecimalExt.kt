package com.tangem.utils.extensions

import java.math.BigDecimal

/**
 * Converts `BigDecimal?` to `BigDecimal`
 *
 * If `BigDecimal?` is `null`, returns `BigDecimal.ZERO`
 */
fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

fun BigDecimal.isZero(): Boolean = this.compareTo(BigDecimal.ZERO) == 0