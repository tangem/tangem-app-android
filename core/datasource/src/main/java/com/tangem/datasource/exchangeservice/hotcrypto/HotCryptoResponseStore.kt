package com.tangem.datasource.exchangeservice.hotcrypto

import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.local.datastore.RuntimeStateStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store of [HotCryptoResponse]
 *
 * @author Andrew Khokhlov on 21/01/2025
 */
@Singleton
class HotCryptoResponseStore @Inject constructor() : RuntimeStateStore<HotCryptoResponse> by RuntimeStateStore(
    defaultValue = HotCryptoResponse(imageHost = null, tokens = emptyList()),
)
