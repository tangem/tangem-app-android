package com.tangem.data.earn.datastore

import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EarnFilterStore @Inject constructor() :
    RuntimeStateStore<EarnFilter> by RuntimeStateStore(
        defaultValue = EarnFilter(
            earnFilterNetwork = EarnFilterNetwork.AllNetworks(isSelected = true),
            earnFilterType = EarnFilterType.ALL,
        ),
    )