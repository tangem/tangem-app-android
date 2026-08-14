package com.tangem.datasource.local.onramp.pairs

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.onramp.model.OnrampPair

internal class DefaultOnrampPairsStore(
    store: RuntimeSharedMapStore<String, List<OnrampPair>>,
) : OnrampPairsStore, RuntimeSharedMapStore<String, List<OnrampPair>> by store