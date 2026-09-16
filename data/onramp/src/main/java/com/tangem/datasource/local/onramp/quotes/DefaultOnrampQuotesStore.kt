package com.tangem.datasource.local.onramp.quotes

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.onramp.model.OnrampQuote

internal class DefaultOnrampQuotesStore(
    store: RuntimeSharedMapStore<String, List<OnrampQuote>>,
) : OnrampQuotesStore, RuntimeSharedMapStore<String, List<OnrampQuote>> by store