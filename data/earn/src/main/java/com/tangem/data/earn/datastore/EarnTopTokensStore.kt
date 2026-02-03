package com.tangem.data.earn.datastore

import arrow.core.Either
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.models.earn.EarnTopToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EarnTopTokensStore @Inject constructor() :
    RuntimeStateStore<EarnTopToken> by RuntimeStateStore(
        defaultValue = Either.Right(emptyList()),
    )