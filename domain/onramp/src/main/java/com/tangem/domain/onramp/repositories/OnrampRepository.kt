package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency

interface OnrampRepository {
    suspend fun fetchCurrencies(): List<OnrampCurrency>
    suspend fun fetchCountries(): List<OnrampCountry>
}