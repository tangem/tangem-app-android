package com.tangem.datasource.local.onramp.sepa

import com.tangem.domain.onramp.model.OnrampCountry

interface OnrampCurrentCountryByIPStore {
    suspend fun getSyncOrNull(): OnrampCountry?
    suspend fun store(value: OnrampCountry)
    suspend fun clear()
}