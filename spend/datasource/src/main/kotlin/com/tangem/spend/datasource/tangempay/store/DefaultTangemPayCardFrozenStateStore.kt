package com.tangem.spend.datasource.tangempay.store

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState

internal class DefaultTangemPayCardFrozenStateStore(
    store: RuntimeSharedMapStore<String, TangemPayCardFrozenState>,
) : TangemPayCardFrozenStateStore, RuntimeSharedMapStore<String, TangemPayCardFrozenState> by store