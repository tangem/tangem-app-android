package com.tangem.domain.models.network

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Crypto currency address
 *
 * @property cryptoCurrency crypto currency
 * @property address        default address
 */
data class CryptoCurrencyAddress(val cryptoCurrency: CryptoCurrency, val address: String)