package com.tangem.datasource.local.onramp.country

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.onramp.model.OnrampCountry

internal class DefaultOnrampCurrentCountryByIPStore(
    private val store: RuntimeSharedStore<OnrampCountry>,
) : OnrampCurrentCountryByIPStore {

    override suspend fun getSyncOrNull(): OnrampCountry? = store.getSyncOrNull()

    override suspend fun store(value: OnrampCountry) = store.store(value)
}