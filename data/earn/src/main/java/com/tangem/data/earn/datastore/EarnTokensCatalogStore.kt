package com.tangem.data.earn.datastore

import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.models.earn.EarnTokensCatalog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EarnTokensCatalogStore @Inject constructor() :
    RuntimeStateStore<EarnTokensCatalog?> by RuntimeStateStore(
        defaultValue = null,
    )