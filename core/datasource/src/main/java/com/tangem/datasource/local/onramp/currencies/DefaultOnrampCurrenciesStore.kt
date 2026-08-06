package com.tangem.datasource.local.onramp.currencies

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.onramp.model.OnrampCurrency

internal class DefaultOnrampCurrenciesStore(
    store: RuntimeSharedMapStore<String, List<OnrampCurrency>>,
) : OnrampCurrenciesStore, RuntimeSharedMapStore<String, List<OnrampCurrency>> by store