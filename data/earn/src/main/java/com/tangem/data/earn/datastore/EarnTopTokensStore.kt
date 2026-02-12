package com.tangem.data.earn.datastore

import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.models.earn.EarnTopToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EarnTopTokensStore @Inject constructor() :
    RuntimeStateStore<EarnTopToken?> by RuntimeStateStore(
        defaultValue = null,
    )