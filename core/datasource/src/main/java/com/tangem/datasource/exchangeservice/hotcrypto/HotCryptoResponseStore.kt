package com.tangem.datasource.exchangeservice.hotcrypto

import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Store of pair of [UserWalletId] and [HotCryptoResponse]
 *
[REDACTED_AUTHOR]
 */
@Singleton
class HotCryptoResponseStore @Inject constructor() :
    RuntimeStateStore<Map<UserWalletId, HotCryptoResponse>> by RuntimeStateStore(defaultValue = emptyMap())