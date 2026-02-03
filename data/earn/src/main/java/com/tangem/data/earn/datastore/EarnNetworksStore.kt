package com.tangem.data.earn.datastore

import arrow.core.Either
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.models.earn.EarnNetworks
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EarnNetworksStore @Inject constructor() :
    RuntimeStateStore<EarnNetworks> by RuntimeStateStore(
        defaultValue = Either.Right(emptyList()),
    )