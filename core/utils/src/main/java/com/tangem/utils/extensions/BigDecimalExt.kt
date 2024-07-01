package com.tangem.utils.extensions

import java.math.BigDecimal

/**
 * Converts `BigDecimal?` to `BigDecimal`
 *
 * If `BigDecimal?` is `null`, returns `BigDecimal.ZERO`
 */
fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO