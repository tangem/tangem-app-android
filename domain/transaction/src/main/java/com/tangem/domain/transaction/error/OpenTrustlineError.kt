package com.tangem.domain.transaction.error

import java.math.BigDecimal

sealed interface OpenTrustlineError {
    val message: String?

    data class SomeError(override val message: String?) : OpenTrustlineError
    data class NotEnoughCoin(val amount: BigDecimal, override val message: String?) : OpenTrustlineError
}