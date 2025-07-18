package com.tangem.tap.features.hot

import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.tap.domain.hot.HotWalletPasswordRequester
import javax.inject.Inject

class DefaultHotWalletPasswordRequester @Inject constructor() : HotWalletPasswordRequester {

    override suspend fun requestPassword(hotWalletId: HotWalletId): HotAuth.Password {
        return HotAuth.Password("TODO [REDACTED_TASK_KEY]".toCharArray()) // TODO [REDACTED_TASK_KEY]
    }
}