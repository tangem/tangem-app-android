package com.tangem.datasource.local.onramp.country

import com.tangem.domain.onramp.model.OnrampCountry

internal interface OnrampCurrentCountryByIPStore {
    suspend fun getSyncOrNull(): OnrampCountry?
    suspend fun store(value: OnrampCountry)
}