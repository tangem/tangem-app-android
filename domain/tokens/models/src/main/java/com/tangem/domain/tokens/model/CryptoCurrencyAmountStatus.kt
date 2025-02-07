package com.tangem.domain.tokens.model

import java.math.BigDecimal

/**
 * Represents possible statuses of cryptocurrency amount.
 */
sealed class CryptoCurrencyAmountStatus {

    data class Loaded(val value: BigDecimal) : CryptoCurrencyAmountStatus()

    data object NotFound : CryptoCurrencyAmountStatus()
}