package com.tangem.datasource.local.visa

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.pay.TangemPayCardFrozenState

internal class DefaultTangemPayCardFrozenStateStore(
    store: RuntimeSharedMapStore<String, TangemPayCardFrozenState>,
) : TangemPayCardFrozenStateStore, RuntimeSharedMapStore<String, TangemPayCardFrozenState> by store