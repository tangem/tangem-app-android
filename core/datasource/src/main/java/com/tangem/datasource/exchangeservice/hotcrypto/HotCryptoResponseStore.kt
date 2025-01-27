package com.tangem.datasource.exchangeservice.hotcrypto

import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.local.datastore.RuntimeStateStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store of [HotCryptoResponse]
 *
[REDACTED_AUTHOR]
 */
@Singleton
class HotCryptoResponseStore @Inject constructor() : RuntimeStateStore<HotCryptoResponse> by RuntimeStateStore(
    defaultValue = HotCryptoResponse(imageHost = null, tokens = emptyList()),
)