package com.tangem.datasource.local.onramp.countries

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.onramp.model.OnrampCountry

internal class DefaultOnrampCountriesStore(
    store: RuntimeSharedMapStore<String, List<OnrampCountry>>,
) : OnrampCountriesStore, RuntimeSharedMapStore<String, List<OnrampCountry>> by store