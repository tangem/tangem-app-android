package com.tangem.tap.domain.hot

import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId

interface HotWalletPasswordRequester {

    suspend fun requestPassword(hotWalletId: HotWalletId): HotAuth.Password
}