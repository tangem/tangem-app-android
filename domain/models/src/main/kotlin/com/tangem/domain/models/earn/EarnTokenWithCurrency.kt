package com.tangem.domain.models.earn

import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.serialization.Serializable

/**
 * Represents an earn token and its associated cryptocurrency. This model is only for main account.
 *
 * @property earnToken The earn token details.
 * @property cryptoCurrency Use this [CryptoCurrency] only for creating the cryptoCurrencyIcon!!!!
 */
@Serializable
data class EarnTokenWithCurrency(
    val networkName: String,
    val earnToken: EarnToken,
    val cryptoCurrency: CryptoCurrency,
)