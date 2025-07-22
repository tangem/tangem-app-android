package com.tangem.domain.transaction.error

import java.math.BigDecimal

sealed interface OpenTrustlineError {

    data class UnknownError(val message: String?) : OpenTrustlineError
    data class SendError(val error: SendTransactionError) : OpenTrustlineError
    data class NotEnoughCoin(val amount: BigDecimal, val symbol: String, val message: String?) : OpenTrustlineError
}